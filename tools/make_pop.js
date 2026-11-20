// Synthesizes a short "pop" sound into app/src/main/res/raw/pop.wav (16-bit mono PCM).
const fs = require('fs');
const path = require('path');

const rate = 44100;
const dur = 0.13;
const n = Math.floor(rate * dur);
const samples = new Int16Array(n);
let phase = 0, phase2 = 0;
for (let i = 0; i < n; i++) {
  const t = i / rate;
  // "Bloop": a bright tone that falls quickly, plus a short airy click at the start.
  const freq = 1500 * Math.exp(-t * 32) + 320;
  phase += (2 * Math.PI * freq) / rate;
  phase2 += (2 * Math.PI * freq * 2.01) / rate;
  const env = Math.min(1, t / 0.0015) * Math.exp(-t * 38);
  const click = i < 140 ? (Math.random() * 2 - 1) * Math.pow(1 - i / 140, 2) * 0.7 : 0;
  const v = (Math.sin(phase) * 0.8 + Math.sin(phase2) * 0.18 * Math.exp(-t * 80)) * env + click;
  samples[i] = Math.max(-1, Math.min(1, v)) * 32767;
}

const data = Buffer.from(samples.buffer);
const h = Buffer.alloc(44);
h.write('RIFF', 0); h.writeUInt32LE(36 + data.length, 4); h.write('WAVE', 8);
h.write('fmt ', 12); h.writeUInt32LE(16, 16); h.writeUInt16LE(1, 20); h.writeUInt16LE(1, 22);
h.writeUInt32LE(rate, 24); h.writeUInt32LE(rate * 2, 28); h.writeUInt16LE(2, 32); h.writeUInt16LE(16, 34);
h.write('data', 36); h.writeUInt32LE(data.length, 40);

const out = path.join(__dirname, '..', 'app', 'src', 'main', 'res', 'raw', 'pop.wav');
fs.mkdirSync(path.dirname(out), { recursive: true });
fs.writeFileSync(out, Buffer.concat([h, data]));
console.log('wrote', out);
