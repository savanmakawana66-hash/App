# Watch Party & Voice Room Relay Server

This server provides the minimum required infrastructure for remote Internet streaming between Android devices in different cities/countries.

## Architecture Overview

1. **Signaling Server**: WebSocket connection facilitating room management, SDP/ICE candidate exchange, and authentication.
2. **Playback Synchronization**: Acts as an authoritative clock relay for Play, Pause, Seek forward, Seek backward, and drift correction (< 50ms sync accuracy).
3. **Voice Communication**: Low-latency 16kHz PCM audio packet routing with active speaker detection and mute states.
4. **Media Streaming Relay**: HTTP byte-range proxy (`Range: bytes=start-end`, `206 Partial Content`) allowing remote friends to stream the Host's local 4+ hour video without downloading the entire file first.

## Running the Server

### Prerequisites
- Node.js (v18 or newer)
- npm

### Installation & Launch
```bash
cd server
npm install
npm start
```
By default, the server runs on port `8080`.

### Environment Variables
- `PORT`: Set custom listening port (default: `8080`).

### Connecting from Android
In the Android app settings or code:
- Local LAN / Emulator: `http://10.0.2.2:8080`
- Production Cloud Server: `wss://your-domain.com` / `https://your-domain.com`
