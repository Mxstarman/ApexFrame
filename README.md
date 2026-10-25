# 🎥 ApexFrame

> **Capture Perfection. Unseen Control.**  
> *A high-performance, clutter-free screen capture application built for creators, gamers, and developers.*

---

## 🌟 Overview

**ApexFrame** is a modern, lightweight screen recorder designed to deliver high-resolution video capture without compromising your workflow. Standard screen recorders force you to choose between awkward keyboard shortcuts or visible control panels that ruin your final recording. 

ApexFrame solves this with **Stealth Overlay Technology**: an interactive floating control ball that sits on your desktop while you record, but **remains completely invisible in the final exported video**. 

Whether you are creating software tutorials, recording high-FPS gameplay, or conducting live presentations with screen annotations, ApexFrame delivers a sleek, zero-clutter recording experience.

---

## ✨ Key Features

### 👻 1. Stealth Overlay Technology (Ghost UI)
* **Invisible Control Ball:** Access real-time controls (Pause, Resume, Stop, Annotate) directly on your screen. The overlay UI is completely filtered out of the capture stream—what you see is total control; what your audience sees is pure content.
* **Draggable & Dockable:** Position the floating ball anywhere on your desktop or dock it to the screen edge as a subtle tab.

### ⏱️ 2. Customizable Pre-Recording Countdown
* **Get Ready:** A visual countdown timer ensures you're prepared before capture begins.
* **Flexible Durations:** Easily adjust or disable the countdown in settings (`0s`, `3s`, `5s`, or `10s`).

### ✏️ 3. Real-Time Screen Annotation Tools
* **Live Drawing & Highlighting:** Draw lines, arrows, callouts, and highlights over any open window during an active recording.
* **Zero Overhead:** Annotation elements automatically disappear when toggled off and leave no residual artifacts on your screen.

### 🎮 4. High-Performance Video & Audio Engine
* **Ultra-Clear Resolution:** Support for 720p, 1080p Full HD, and 4K recording.
* **High Frame Rates:** Silky-smooth 30 FPS, 60 FPS, and high-refresh-rate capture (90/120 FPS).
* **Dual Audio Mixer:** Simultaneously capture internal system audio and microphone input with individual volume controls and noise suppression.
* **Custom Bitrates:** Adjustable video bitrates (up to 50+ Mbps) and container formats (`MP4` / `MKV`).

### ⌨️ 5. Global Hotkeys & Custom Shortcuts
* **Instant Command:** Control recording states with global keyboard hotkeys:
  * `Start / Stop Recording`
  * `Pause / Resume`
  * `Toggle Floating Ball Visibility`
  * `Clear Screen Annotations`

---

## 🛠️ Architecture & Stealth Engine

ApexFrame isolates the user interface layer from the desktop capture pipeline. By configuring native window capture attributes (such as `SetWindowDisplayAffinity` on Windows or separate surface projection layers on Android/macOS), the capture engine records the desktop composition *beneath* ApexFrame's UI elements.

┌──────────────────────────────────────────────────┐
│              User Display (Screen)               │
│  ┌──────────────────┐    ┌────────────────────┐  │
│  │ Desktop Content  │    │ Floating Ball UI   │  │
│  └────────┬─────────┘    └─────────┬──────────┘  │
└───────────┼────────────────────────┼─────────────┘
│ (Captured)             │ (Excluded via Hardware Layer)
▼                        ▼
┌───────────────────────┐  ┌──────────────────┐
│ Video Encoder Stream  │  │ User Interaction │
│   (Final Output)      │  │    & Controls    │
└───────────────────────┘  └──────────────────┘

---

## ⚙️ Configuration Options

ApexFrame comes pre-configured with optimized defaults, but allows deep customization via the settings dashboard:

| Setting | Default | Options |
| :--- | :--- | :--- |
| **Resolution** | 1080p (Full HD) | 720p, 1080p, 4K, Native |
| **Frame Rate** | 60 FPS | 30 FPS, 60 FPS, 120 FPS |
| **Countdown Timer** | 3 Seconds | Off (0s), 3s, 5s, 10s |
| **Video Format** | MP4 (H.264) | MP4, MKV, WebM |
| **Audio Capture** | System + Mic | System Only, Mic Only, Muted |
| **Floating Ball** | Enabled (Stealth) | Enabled, Hidden, Classic Mode |

---

## 🚀 Getting Started

### Prerequisites
* Operating System: Windows 10/11, macOS 12+, or Linux / Android 8.0+
* Minimum Hardware: Intel i5 / Ryzen 5 or equivalent, 4GB RAM, GPU with Hardware Acceleration (NVENC / QuickSync / AMF recommended)

### Installation
1. Head over to the **[Releases](../../releases)** tab.
2. Download the latest installer or binary package for your operating system.
3. Launch `ApexFrame` and configure your preferred storage directory and output format.
4. Press `Rec` or your global hotkey to begin capturing!

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the [Issues](../../issues) page if you'd like to report a bug or suggest an enhancement.

---

## 📄 License

ApexFrame End-User & Source License
Copyright (c) 2026 Mxstarman. All rights reserved.

1. PERMITTED USE
You are granted a non-exclusive, non-transferable license to download, install, 
compile, and run ApexFrame solely for your own personal, educational, or 
internal non-commercial use.

2. RESTRICTIONS
You may NOT:
a) Re-publish, re-distribute, mirror, host, upload, or sub-license the binary, 
   executable, or source code to any public or private app store, website, 
   repository, file-sharing network, or distribution platform.
b) Sell, rent, lease, monetize, or include ApexFrame (in whole or in part) in any 
   commercial software, product, or service.
c) Remove, obscure, or alter any copyright notices, branding, or logos contained 
   within the software or source files.

3. MODIFICATIONS FOR PERSONAL USE
You may modify the source code solely for your own personal local use. Any 
modified version remains subject to all restrictions in Section 2, and modified 
versions MAY NOT be published or distributed publicly.

4. DISCLAIMER OF WARRANTY
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY.
