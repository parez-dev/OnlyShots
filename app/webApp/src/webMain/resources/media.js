// The browser's half of the web editor. The editor itself is the shared Compose editor compiled to wasm; this file
// does only what Kotlin cannot reach from there: the files the user picked, playing a video with its sound, decoding
// frames, thumbnails and audio, and encoding the MP4. Frames go straight into and out of Skia's memory (see
// WebMedia.kt), and the compositing (blur, glass, the graph) stays in Kotlin with the same Skia code as iOS.
//
// Mediabunny (MPL-2.0, unmodified) demuxes and decodes the picked file and muxes the MP4. WebCodecs does the encoding,
// which is why an export needs Chrome, Edge or Safari 16.4+.
import {
  ALL_FORMATS,
  AudioSampleSink,
  AudioSampleSource,
  BlobSource,
  BufferTarget,
  CanvasSource,
  Input,
  Mp4OutputFormat,
  Output,
  QUALITY_HIGH,
  QUALITY_MEDIUM,
  VideoSampleSink,
  canEncodeAudio,
} from 'https://cdn.jsdelivr.net/npm/mediabunny@1.61.4/+esm';

// Files ---------------------------------------------------------------------------------------------------------------

/** The session's files by the path Kotlin knows them by: picked videos and shots, finished exports. */
const files = new Map();
let picked = 0;

function register(path, blob) {
  files.set(path, blob);
}

function alias(from, to) {
  const blob = files.get(from);
  if (blob) files.set(to, blob);
  return !!blob;
}

function forget(path) {
  files.delete(path);
  frameSinks.delete(path);
  lumaWalks.get(path)?.current?.close();
  lumaWalks.delete(path);
}

function fileOf(path) {
  const blob = files.get(path);
  if (!blob) throw new Error(`no file at ${path}`);
  return blob;
}

/** Asks for one file; resolves with the path it is registered under, or null when the dialog is closed. */
function pickFile(accept) {
  return new Promise(resolve => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = accept;
    input.style.display = 'none';
    let settled = false;
    const done = path => {
      if (settled) return;
      settled = true;
      input.remove();
      resolve(path);
    };
    input.addEventListener('change', () => {
      const file = input.files?.[0];
      done(file ? registerPicked(file) : null);
    });
    input.addEventListener('cancel', () => done(null));
    document.body.appendChild(input);
    input.click();
  });
}

function registerPicked(file) {
  const path = `/picked/${++picked}/${file.name.replace(/[\\/]/g, '_')}`;
  files.set(path, file);
  return path;
}

/**
 * Files dropped on the page go to [onFile] by path, videos only; [onHover] hears whether a file is being dragged over
 * the page. Enter and leave fire for every element crossed, so a depth count tells the real leave.
 */
function onDrop(onFile, onHover) {
  let depth = 0;
  const carriesFiles = e => [...(e.dataTransfer?.types ?? [])].includes('Files');
  window.addEventListener('dragenter', e => {
    if (!carriesFiles(e)) return;
    e.preventDefault();
    if (depth++ === 0) onHover(true);
  });
  window.addEventListener('dragover', e => e.preventDefault());
  window.addEventListener('dragleave', e => {
    if (!carriesFiles(e)) return;
    if (--depth <= 0) {
      depth = 0;
      onHover(false);
    }
  });
  window.addEventListener('drop', e => {
    e.preventDefault();
    e.stopPropagation();
    depth = 0;
    onHover(false);
    const file = e.dataTransfer?.files?.[0];
    if (file) onFile(registerPicked(file));
  });
}

function readText(path) {
  return fileOf(path).text();
}

function download(path, name) {
  const url = URL.createObjectURL(fileOf(path));
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  a.click();
  // Revoked late: Safari needs the object URL alive while the download starts.
  setTimeout(() => URL.revokeObjectURL(url), 60_000);
}

// Probing -------------------------------------------------------------------------------------------------------------

function inputOf(path) {
  return new Input({ formats: ALL_FORMATS, source: new BlobSource(fileOf(path)) });
}

/**
 * What one open of a file says about it. A file this cannot read answers `canDecode: false` instead of throwing: a
 * rejected promise reaches Kotlin/Wasm with nothing left of it but the word Error, and a file the user picked wrongly
 * is an answer, not a fault. `codec` is Mediabunny's name for it ('hevc', 'prores'…), empty when it found no video.
 */
async function probe(path) {
  const input = inputOf(path);
  // Anything that is not a container this knows throws out of the demuxer; that is the same wrong pick.
  const track = await input.getPrimaryVideoTrack().catch(() => null);
  if (!track) return { canDecode: false, codec: '' };
  if (!(await track.canDecode())) return { canDecode: false, codec: track.codec || '' };
  let created = 0;
  try {
    const tags = await input.getMetadataTags();
    const raw = tags?.date ?? tags?.creationTime;
    const parsed = raw instanceof Date ? raw.getTime() : raw ? Date.parse(raw) : NaN;
    if (!Number.isNaN(parsed)) created = parsed;
  } catch {
    created = 0; // containers without tags are the common case, not an error
  }
  return {
    canDecode: true,
    duration: await input.computeDuration(),
    hasAudio: !!(await input.getPrimaryAudioTrack()),
    width: track.displayWidth,
    height: track.displayHeight,
    // Epoch milliseconds from the file's own metadata, 0 when it carries none.
    created,
  };
}

/** Cover, not stretch: the sample scaled to fill w × h, centred, then zoomed and moved by the reframe. */
function drawCover(sample, ctx, w, h, zoom = 1, offsetX = 0, offsetY = 0) {
  const scale = Math.max(w / sample.displayWidth, h / sample.displayHeight) * zoom;
  const dw = sample.displayWidth * scale;
  const dh = sample.displayHeight * scale;
  ctx.clearRect(0, 0, w, h);
  sample.draw(ctx, (w - dw) / 2 + offsetX * dw, (h - dh) / 2 + offsetY * dh, dw, dh);
}

/**
 * One decoder per file for single frames (thumbnails, brightness), kept for the session: opening a file is the slow
 * part, and the brightness track asks for a frame every few hundred milliseconds of the clip.
 */
const frameSinks = new Map();

async function sinkOf(path) {
  let sink = frameSinks.get(path);
  if (!sink) {
    sink = inputOf(path)
      .getPrimaryVideoTrack()
      .then(track => new VideoSampleSink(track));
    frameSinks.set(path, sink);
  }
  return sink;
}

/** A JPEG of the frame at [time] seconds, at most [maxWidth] wide, as bytes. */
async function thumbnail(path, time, maxWidth) {
  const sample = await (await sinkOf(path)).getSample(time);
  if (!sample) throw new Error('no frame there');
  const scale = Math.min(1, maxWidth / sample.displayWidth);
  const canvas = new OffscreenCanvas(
    Math.max(1, Math.round(sample.displayWidth * scale)),
    Math.max(1, Math.round(sample.displayHeight * scale)),
  );
  sample.draw(canvas.getContext('2d'), 0, 0, canvas.width, canvas.height);
  sample.close();
  const blob = await canvas.convertToBlob({ type: 'image/jpeg', quality: 0.8 });
  return new Uint8Array(await blob.arrayBuffer());
}

/** The first [maxSeconds] of sound, mixed to mono, or null without sound. */
async function decodeAudio(path, maxSeconds) {
  const Ctx = globalThis.AudioContext ?? globalThis.webkitAudioContext;
  if (!Ctx) return null;
  const context = new Ctx();
  try {
    const buffer = await context.decodeAudioData(await fileOf(path).arrayBuffer());
    const length = Math.min(buffer.length, Math.floor(buffer.sampleRate * maxSeconds));
    const mono = new Float32Array(length);
    for (let c = 0; c < buffer.numberOfChannels; c++) {
      const data = buffer.getChannelData(c);
      for (let i = 0; i < length; i++) mono[i] += data[i] / buffer.numberOfChannels;
    }
    return { rate: buffer.sampleRate, samples: mono };
  } catch {
    return null; // a clip recorded without sound, or a codec this browser will not decode
  } finally {
    await context.close();
  }
}

/**
 * Brightness is asked for frame after frame, forwards through the clip (the brightness track), so each file keeps a
 * walk through its decoded frames and moves it on; a jump back starts a new walk. Seeking for every sample would
 * decode from the previous keyframe each time.
 */
const lumaWalks = new Map();

async function frameAtWalking(path, time) {
  let walk = lumaWalks.get(path);
  if (!walk || time < walk.time) {
    walk?.current?.close();
    const sink = await sinkOf(path);
    walk = { samples: sink.samples(time)[Symbol.asyncIterator](), current: null, next: null, time };
    lumaWalks.set(path, walk);
  }
  walk.time = time;
  // Move on while the next frame starts at or before [time].
  for (;;) {
    walk.next ??= (await walk.samples.next()).value ?? null;
    if (!walk.next) break;
    if (walk.current && walk.next.timestamp > time) break;
    walk.current?.close();
    walk.current = walk.next;
    walk.next = null;
  }
  return walk.current;
}

/** Luma (0..1) at each point (x, y pairs, fractions of the frame) of the frame at [time], or null. */
async function lumaAt(path, time, points) {
  const sample = await frameAtWalking(path, time);
  if (!sample) return null;
  const w = 160;
  const h = Math.max(1, Math.round((w * sample.displayHeight) / sample.displayWidth));
  const ctx = new OffscreenCanvas(w, h).getContext('2d', { willReadFrequently: true });
  sample.draw(ctx, 0, 0, w, h);
  const pixels = ctx.getImageData(0, 0, w, h).data;
  const out = new Float32Array(points.length / 2);
  for (let i = 0; i < out.length; i++) {
    const x = Math.min(w - 1, Math.max(0, Math.floor(points[2 * i] * w)));
    const y = Math.min(h - 1, Math.max(0, Math.floor(points[2 * i + 1] * h)));
    const o = (y * w + x) * 4;
    out[i] = (0.2126 * pixels[o] + 0.7152 * pixels[o + 1] + 0.0722 * pixels[o + 2]) / 255;
  }
  return out;
}

// Playback ------------------------------------------------------------------------------------------------------------

/**
 * A hidden <video> keeps the clock and plays the sound; its picture is copied into a canvas of at most [maxSide] for
 * Compose to draw, so Haze can blur it like the phones' picture.
 */
const players = new Map();
let nextPlayer = 0;

/**
 * Players that have not had their one gesture yet. iOS gives an element the right to sound only when play() runs
 * inside a user gesture, and only for that element; the editor plays from a coroutine a moment after the tap, which
 * is too late for that rule. So every pointer the page sees starts and stops the waiting elements, which is enough
 * to bless them: the tap on the play button blesses the player just before the editor asks it to run.
 */
const unblessed = new Set();

function bless(player) {
  unblessed.delete(player);
  const wanted = player.video.muted;
  player.video.muted = false;
  let started;
  try {
    started = player.video.play();
  } catch {
    player.video.muted = wanted;
    unblessed.add(player);
    return;
  }
  // Stopped in the same task, so the blessing itself is never heard; that aborts the play, which is not a refusal.
  player.video.pause();
  player.video.muted = wanted;
  if (started) {
    started.catch(e => {
      if (e && e.name === 'NotAllowedError') unblessed.add(player);
    });
  }
}

// Capture, because Compose handles the canvas's own events: the editor's play button is a tap on that canvas.
document.addEventListener('pointerdown', () => unblessed.forEach(bless), { capture: true });

function playerOpen(path, maxSide) {
  const video = document.createElement('video');
  video.src = URL.createObjectURL(fileOf(path));
  video.preload = 'auto';
  video.playsInline = true;
  video.muted = false;
  // Kept in the document and barely visible: a detached or hidden video stops handing out frames in some browsers.
  Object.assign(video.style, {
    position: 'fixed',
    left: '0',
    top: '0',
    width: '2px',
    height: '2px',
    opacity: '0.01',
    pointerEvents: 'none',
  });
  document.body.appendChild(video);
  // `muted` is what the editor asked for; the element's own flag may differ while autoplay refuses the sound.
  const player = { video, maxSide, canvas: null, ctx: null, fresh: false, muted: false };
  unblessed.add(player);
  const watch = () => {
    player.fresh = true;
    video.requestVideoFrameCallback?.(watch);
  };
  if (video.requestVideoFrameCallback) video.requestVideoFrameCallback(watch);
  else video.addEventListener('timeupdate', () => (player.fresh = true));
  video.addEventListener('seeked', () => (player.fresh = true));
  video.addEventListener('loadeddata', () => (player.fresh = true));
  // A paused, all but invisible video may never start loading by itself.
  video.load();
  players.set(++nextPlayer, player);
  return nextPlayer;
}

function playerClose(id) {
  const p = players.get(id);
  if (!p) return;
  unblessed.delete(p);
  p.video.pause();
  URL.revokeObjectURL(p.video.src);
  p.video.remove();
  players.delete(id);
}

function playerSeek(id, time) {
  const p = players.get(id);
  if (!p) return Promise.resolve(false);
  return new Promise(resolve => {
    const v = p.video;
    const go = () => {
      v.addEventListener('seeked', () => resolve(true), { once: true });
      v.currentTime = time;
    };
    if (v.readyState >= 1) go();
    else v.addEventListener('loadedmetadata', go, { once: true });
  });
}

async function playerPlay(id) {
  const p = players.get(id);
  if (!p) return false;
  // Autoplay rules: a muted start is always allowed, an audible one only once the page has been clicked. What the
  // editor asked for is tried again at every play, so a start that had to be silent is not silent for ever.
  p.video.muted = p.muted;
  try {
    await p.video.play();
    return true;
  } catch {
    p.video.muted = true;
    await p.video.play().catch(() => {});
    return false;
  }
}

function playerPause(id) {
  players.get(id)?.video.pause();
}

function playerTime(id) {
  return players.get(id)?.video.currentTime ?? 0;
}

function playerMute(id, muted) {
  const p = players.get(id);
  if (!p) return;
  p.muted = muted;
  p.video.muted = muted;
}

/**
 * Draws a new frame into the player's canvas and returns true, or false when there is none since the last call. Its
 * size is then in playerWidth/playerHeight, and playerPixelsInto copies it out.
 */
function playerFrame(id) {
  const p = players.get(id);
  if (!p || !p.fresh || p.video.readyState < 2 || !p.video.videoWidth) return false;
  p.fresh = false;
  const vw = p.video.videoWidth;
  const vh = p.video.videoHeight;
  const scale = Math.min(1, p.maxSide / Math.max(vw, vh));
  const w = Math.max(1, Math.round(vw * scale));
  const h = Math.max(1, Math.round(vh * scale));
  if (!p.canvas || p.canvas.width !== w || p.canvas.height !== h) {
    p.canvas = new OffscreenCanvas(w, h);
    p.ctx = p.canvas.getContext('2d', { willReadFrequently: true });
  }
  p.ctx.drawImage(p.video, 0, 0, w, h);
  p.width = w;
  p.height = h;
  return true;
}

/** Writes the player's last frame, RGBA, into [memory] at [address]. */
function playerPixelsInto(id, memory, address) {
  const p = players.get(id);
  const data = p.ctx.getImageData(0, 0, p.width, p.height).data;
  new Uint8Array(memory, address, data.length).set(data);
}

function playerWidth(id) {
  return players.get(id)?.width ?? 0;
}

function playerHeight(id) {
  return players.get(id)?.height ?? 0;
}

// Export --------------------------------------------------------------------------------------------------------------

const exp = {
  input: null,
  samples: null,
  sample: null,
  frame: null, // the decoded frame, reframed and cover-filled at the output size, for Kotlin to composite on
  frameCtx: null,
  canvas: null, // the composited frame Kotlin hands back, which the encoder reads
  ctx: null,
  output: null,
  source: null,
  audio: null, // { sink, source }: the clip's sound, re-encoded into the export at the end
  start: 0,
  end: 0,
  zoom: 1,
  offsetX: 0,
  offsetY: 0,
  cancelled: false,
};

/**
 * Starts walking the decoded frames of [start, end] seconds and an MP4 of w × h. The picture is drawn cover-filled and
 * reframed here, at the output size, so Kotlin never handles a 4K source frame. Resolves with the frame rate.
 */
async function exportStart(path, w, h, start, end, zoom, offsetX, offsetY, withSound) {
  exportReset();
  exp.input = inputOf(path);
  const track = await exp.input.getPrimaryVideoTrack();
  const stats = await track.computePacketStats(100);
  const fps = stats.averagePacketRate || 30;
  exp.samples = new VideoSampleSink(track).samples(start, end)[Symbol.asyncIterator]();
  exp.frame = new OffscreenCanvas(w, h);
  exp.frameCtx = exp.frame.getContext('2d', { willReadFrequently: true });
  exp.canvas = new OffscreenCanvas(w, h);
  exp.ctx = exp.canvas.getContext('2d');
  Object.assign(exp, { zoom, offsetX, offsetY, start, end, cancelled: false });
  exp.output = new Output({ format: new Mp4OutputFormat({ fastStart: 'in-memory' }), target: new BufferTarget() });
  exp.source = new CanvasSource(exp.canvas, { codec: 'avc', bitrate: QUALITY_HIGH });
  exp.output.addVideoTrack(exp.source, { frameRate: fps });
  // The sound is decoded and encoded again as AAC, which an MP4 holds whatever the source carried. A browser with no
  // AAC encoder (and WebCodecs reports that per codec) exports the picture alone rather than failing the whole video.
  const sound = withSound ? await exp.input.getPrimaryAudioTrack().catch(() => null) : null;
  if (sound && (await canEncodeAudio('aac'))) {
    const source = new AudioSampleSource({ codec: 'aac', bitrate: QUALITY_MEDIUM });
    exp.output.addAudioTrack(source);
    exp.audio = { sink: new AudioSampleSink(sound), source };
  }
  await exp.output.start();
  return fps;
}

/**
 * Writes the clip's sound into the export, shifted so it starts where the trim does. Done in one pass at the end: the
 * MP4 is built in memory until finalize, so nothing depends on the two tracks arriving interleaved.
 *
 * ponytail: a sample straddling the trim's start goes in whole, which can carry a few milliseconds of what came
 * before. AudioSample.trim() would cut it exactly, for a frame of sound nobody hears.
 */
async function exportAddSound() {
  if (!exp.audio) return;
  for await (const sample of exp.audio.sink.samples(exp.start, exp.end)) {
    if (exp.cancelled) {
      sample.close();
      return;
    }
    sample.setTimestamp(Math.max(0, sample.timestamp - exp.start));
    await exp.audio.source.add(sample);
    sample.close();
  }
}

/** Decodes the next frame; resolves with its time in seconds, or -1 at the end. */
async function exportNext() {
  if (!exp.samples || exp.cancelled) return -1;
  exp.sample?.close();
  exp.sample = null;
  const next = await exp.samples.next();
  if (next.done) return -1;
  exp.sample = next.value;
  drawCover(exp.sample, exp.frameCtx, exp.frame.width, exp.frame.height, exp.zoom, exp.offsetX, exp.offsetY);
  return exp.sample.timestamp;
}

/** Writes the decoded frame's RGBA pixels into [memory] (an ArrayBuffer) at [address]: a Skia bitmap's pixels. */
function exportFrameInto(memory, address) {
  const data = exp.frameCtx.getImageData(0, 0, exp.frame.width, exp.frame.height).data;
  new Uint8Array(memory, address, data.length).set(data);
}

/** Encodes the composited frame, RGBA in [memory] at [address], at [timestamp] seconds into the export. */
async function exportAddFrom(memory, address, timestamp, duration) {
  const w = exp.canvas.width;
  const h = exp.canvas.height;
  const pixels = new Uint8ClampedArray(memory.slice(address, address + w * h * 4));
  exp.ctx.putImageData(new ImageData(pixels, w, h), 0, 0);
  await exp.source.add(timestamp, duration);
  return true;
}

/** Finishes the MP4 and registers it under [outPath]. */
async function exportFinish(outPath) {
  await exportAddSound();
  await exp.output.finalize();
  files.set(outPath, new Blob([exp.output.target.buffer], { type: 'video/mp4' }));
  exportReset();
  return true;
}

async function exportCancel() {
  exp.cancelled = true;
  try {
    await exp.output?.cancel();
  } catch {
    // Already finalised, or never started: either way there is nothing left to drop.
  }
  exportReset();
  return true;
}

function exportReset() {
  exp.sample?.close();
  Object.assign(exp, { input: null, samples: null, sample: null, output: null, source: null, audio: null });
}

globalThis.cg = {
  register,
  alias,
  forget,
  pickFile,
  onDrop,
  readText,
  download,
  probe,
  thumbnail,
  decodeAudio,
  lumaAt,
  playerOpen,
  playerClose,
  playerSeek,
  playerPlay,
  playerPause,
  playerTime,
  playerMute,
  playerFrame,
  playerPixelsInto,
  playerWidth,
  playerHeight,
  exportStart,
  exportNext,
  exportFrameInto,
  exportAddFrom,
  exportFinish,
  exportCancel,
};
