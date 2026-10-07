package com.squig.equalizer.web

import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpExchange
import java.net.InetSocketAddress
import java.io.OutputStream
import java.util.concurrent.Executors

class WebServer(private val port: Int = 8080) {

    fun start() {
        val server = HttpServer.create(InetSocketAddress(port), 0)
        server.createContext("/", DashboardHandler())
        server.executor = Executors.newFixedThreadPool(4)
        server.start()
        println("=====================================================")
        println(" SquigEqualizer Web GUI running at: http://localhost:$port")
        println("=====================================================")
    }

    class DashboardHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>SquigEqualizer</title>
                <style>
                    :root {
                        --bg-main: #121216;
                        --bg-card: #1a1a22;
                        --bg-input: #242430;
                        --accent: #7c3aed;
                        --accent-hover: #6d28d9;
                        --text-main: #f3f4f6;
                        --text-dim: #9ca3af;
                        --border: #2d2d3a;
                        --target-color: #ef4444;
                        --iem-color: #3b82f6;
                    }
                    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
                    body { background: var(--bg-main); color: var(--text-main); min-height: 100vh; padding: 16px; }
                    
                    /* Header */
                    .app-header {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        padding: 14px 18px;
                        background: var(--bg-card);
                        border: 1px solid var(--border);
                        border-radius: 12px;
                        margin-bottom: 24px;
                    }
                    .app-title { font-size: 18px; font-weight: 700; }
                    .icon-btn { background: transparent; border: none; color: var(--text-dim); font-size: 20px; cursor: pointer; }

                    /* Initial State Screen */
                    .initial-container {
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        justify-content: center;
                        padding: 60px 20px;
                        text-align: center;
                    }
                    .btn-add-main {
                        background: var(--accent);
                        color: #fff;
                        border: none;
                        padding: 16px 32px;
                        font-size: 16px;
                        font-weight: 700;
                        border-radius: 12px;
                        cursor: pointer;
                        box-shadow: 0 4px 14px rgba(124, 58, 237, 0.4);
                        transition: transform 0.2s, background 0.2s;
                    }
                    .btn-add-main:hover { background: var(--accent-hover); transform: translateY(-2px); }

                    /* Profile List View */
                    .profile-card {
                        background: var(--bg-card);
                        border: 1px solid var(--border);
                        border-radius: 12px;
                        padding: 16px;
                        margin-bottom: 12px;
                        cursor: pointer;
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                    }
                    .profile-name { font-weight: 600; font-size: 16px; }
                    .profile-sub { font-size: 12px; color: var(--text-dim); margin-top: 4px; }

                    /* Modal Dialog */
                    .modal-backdrop {
                        position: fixed;
                        top: 0; left: 0; right: 0; bottom: 0;
                        background: rgba(0,0,0,0.8);
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        padding: 16px;
                        z-index: 1000;
                    }
                    .modal-content {
                        background: var(--bg-card);
                        border: 1px solid var(--border);
                        border-radius: 16px;
                        width: 100%;
                        max-width: 650px;
                        max-height: 90vh;
                        overflow-y: auto;
                        padding: 20px;
                    }
                    .modal-header {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        margin-bottom: 16px;
                    }

                    .form-group { margin-bottom: 16px; position: relative; }
                    label { display: block; font-size: 12px; text-transform: uppercase; color: var(--text-dim); margin-bottom: 6px; }
                    input[type="text"], select {
                        width: 100%;
                        padding: 12px;
                        background: var(--bg-input);
                        border: 1px solid var(--border);
                        border-radius: 8px;
                        color: #fff;
                        font-size: 14px;
                    }

                    /* Search Dropdown Results List */
                    .search-results {
                        position: absolute;
                        top: 100%;
                        left: 0;
                        right: 0;
                        max-height: 180px;
                        overflow-y: auto;
                        background: var(--bg-input);
                        border: 1px solid var(--border);
                        border-radius: 0 0 8px 8px;
                        z-index: 10;
                    }
                    .search-item {
                        padding: 10px 14px;
                        font-size: 13px;
                        cursor: pointer;
                        border-bottom: 1px solid rgba(255,255,255,0.05);
                    }
                    .search-item:hover {
                        background: var(--accent);
                        color: #fff;
                    }

                    /* Preamp Control */
                    .preamp-box {
                        background: var(--bg-input);
                        border: 1px solid var(--border);
                        padding: 12px 16px;
                        border-radius: 8px;
                        display: flex;
                        align-items: center;
                        gap: 12px;
                    }
                    .preamp-val { font-weight: 700; color: #10b981; font-size: 16px; min-width: 70px; text-align: right; }

                    /* Graph Canvas */
                    canvas {
                        width: 100%;
                        height: 220px;
                        background: #0d0d12;
                        border: 1px solid var(--border);
                        border-radius: 8px;
                        margin-top: 8px;
                    }

                    .legend { display: flex; gap: 16px; font-size: 12px; margin-top: 8px; }
                    .legend-item { display: flex; align-items: center; gap: 6px; }
                    .dot-target { width: 10px; height: 10px; background: var(--target-color); border-radius: 50%; }
                    .dot-iem { width: 10px; height: 10px; background: var(--iem-color); border-radius: 50%; }

                    .hidden { display: none !important; }
                    .btn-secondary { background: var(--bg-input); border: 1px solid var(--border); color: #fff; padding: 10px 16px; border-radius: 8px; cursor: pointer; }
                </style>
            </head>
            <body>

                <!-- App Header -->
                <div class="app-header">
                    <span class="app-title">SquigEqualizer</span>
                    <button class="icon-btn" onclick="openSettings()">⚙️</button>
                </div>

                <!-- Initial Empty View -->
                <div id="initialView" class="initial-container">
                    <button class="btn-add-main" onclick="openProfileModal()">+ Add Profile</button>
                </div>

                <!-- Profile List Container -->
                <div id="profileList" class="hidden"></div>

                <!-- Profile Details & Squig.link Modal -->
                <div id="profileModal" class="modal-backdrop hidden">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h3 id="modalTitle">Profile Details</h3>
                            <button class="icon-btn" onclick="closeProfileModal()">✕</button>
                        </div>

                        <div class="form-group">
                            <label>Profile Name</label>
                            <input type="text" id="profileName" placeholder="e.g. My KZ Castor Harman EQ">
                        </div>

                        <!-- Squig.link IEM Search -->
                        <div class="form-group">
                            <label>Search Squig.link IEM Database</label>
                            <input type="text" id="modelSearchInput" placeholder="Type model (e.g. Castor 1000, Tangzu Wan'er, Chu II)..." oninput="filterModels(this.value)" onfocus="filterModels(this.value)">
                            <div id="searchResults" class="search-results hidden"></div>
                        </div>

                        <div class="form-group">
                            <label>Target Curve Profile</label>
                            <select id="targetSelect" onchange="updateSquigGraph()">
                                <option value="harman2019">Harman 2019 IE Target</option>
                                <option value="ief">IEF Neutral Target</option>
                                <option value="jm1">JM-1 Target</option>
                                <option value="knowles">Knowles Target</option>
                            </select>
                        </div>

                        <!-- Synchronized Preamp Control -->
                        <div class="form-group">
                            <label>Synchronized Preamp Gain (From Squig.link)</label>
                            <div class="preamp-box">
                                <input type="range" id="preampSlider" style="flex:1;" min="-20" max="5" step="0.5" value="0" oninput="syncPreamp(this.value)">
                                <span id="preampVal" class="preamp-val">0.0 dB</span>
                            </div>
                        </div>

                        <!-- FR Canvas Preview -->
                        <div class="form-group">
                            <label>Squig.link Response Preview</label>
                            <canvas id="squigCanvas"></canvas>
                            <div class="legend">
                                <div class="legend-item"><div class="dot-iem"></div><span id="labelIem">IEM Response</span></div>
                                <div class="legend-item"><div class="dot-target"></div><span>Selected Target</span></div>
                            </div>
                        </div>

                        <div style="display:flex; justify-content: flex-end; gap:8px; margin-top:20px;">
                            <button class="btn-secondary" onclick="closeProfileModal()">Cancel</button>
                            <button class="btn-add-main" style="padding:10px 20px; font-size:14px;" onclick="saveProfile()">Save Profile</button>
                        </div>
                    </div>
                </div>

                <script>
                    let profiles = [];
                    let currentPreamp = 0.0;
                    let selectedIemName = "Generic IEM";

                    // Database with KZ Castor hardware switch positions and popular Squig.link models
                    const squigDatabase = [
                        // KZ Castor Silver (Harman Edition) - Switches: Bass1 Bass2 Treble1 Treble2
                        "KZ Castor Harman (0000 - All Switches Off)",
                        "KZ Castor Harman (1000 - Bass +1dB)",
                        "KZ Castor Harman (0100 - Bass +2dB)",
                        "KZ Castor Harman (1100 - Bass +3dB)",
                        "KZ Castor Harman (0010 - Treble +1dB)",
                        "KZ Castor Harman (0001 - Treble +2dB)",
                        "KZ Castor Harman (0011 - Treble +3dB)",
                        "KZ Castor Harman (1010 - Bass +1dB / Treble +1dB)",
                        "KZ Castor Harman (1111 - All Switches On)",
                        // KZ Castor Black (Bass Enhanced Edition)
                        "KZ Castor Bass (0000 - All Switches Off)",
                        "KZ Castor Bass (1000 - Sub-Bass Boost 1)",
                        "KZ Castor Bass (0100 - Sub-Bass Boost 2)",
                        "KZ Castor Bass (1100 - Max Bass Boost)",
                        "KZ Castor Bass (0010 - Treble Boost 1)",
                        "KZ Castor Bass (0001 - Treble Boost 2)",
                        "KZ Castor Bass (1111 - All Switches On)",
                        // Popular Squig.link Database Entries
                        "7Hz Salnotes Zero",
                        "7Hz Zero:2",
                        "7Hz Timeless",
                        "7Hz Legato",
                        "AFUL Performer 5",
                        "AFUL Performer 8",
                        "BLESSING 2 Dusk",
                        "BLESSING 3",
                        "DUNU Titan S",
                        "DUNU SA6 MK2",
                        "Kiwi Ears Cadenza",
                        "Kiwi Ears Quintet",
                        "KZ ZSN Pro X",
                        "KZ PR2 Planar",
                        "Letshuoer S12",
                        "Letshuoer S12 Pro",
                        "Moondrop Chu",
                        "Moondrop Chu II",
                        "Moondrop Aria",
                        "Moondrop Aria 2",
                        "Moondrop Kato",
                        "Moondrop Variation",
                        "Simgot EA500",
                        "Simgot EA500 LM",
                        "Simgot EM6L",
                        "Simgot SuperMix 4",
                        "Tangzu Wan'er S.G",
                        "Tangzu Wan'er S.G SE",
                        "Truthear Hola",
                        "Truthear ZERO",
                        "Truthear x Crinacle ZERO:RED",
                        "Truthear Nova",
                        "Truthear Hexa"
                    ];

                    const isoFreqs = [20, 50, 100, 200, 500, 1000, 2000, 3000, 5000, 8000, 16000];
                    const targets = {
                        harman2019: [8.5, 7.8, 5.5, 2.0, 0.0, 0.0, 8.0, 7.0, 2.0, 0.0, -6.0],
                        ief: [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 7.0, 4.0, 1.0, 0.0, -8.0],
                        jm1: [6.0, 5.5, 5.0, 2.0, 0.0, 0.0, 8.5, 6.0, 3.0, 1.0, -4.0],
                        knowles: [5.0, 4.5, 4.0, 1.5, 0.0, 0.0, 12.0, 8.0, 6.0, 4.0, -2.0]
                    };

                    function openProfileModal() {
                        document.getElementById('profileName').value = '';
                        document.getElementById('modelSearchInput').value = '';
                        document.getElementById('searchResults').classList.add('hidden');
                        document.getElementById('profileModal').classList.remove('hidden');
                        updateSquigGraph();
                    }

                    function closeProfileModal() {
                        document.getElementById('profileModal').classList.add('hidden');
                    }

                    function filterModels(query) {
                        const resultsContainer = document.getElementById('searchResults');
                        resultsContainer.innerHTML = '';

                        const filtered = squigDatabase.filter(m => m.toLowerCase().includes(query.toLowerCase()));

                        if (filtered.length === 0) {
                            resultsContainer.classList.add('hidden');
                            return;
                        }

                        filtered.forEach(model => {
                            const div = document.createElement('div');
                            div.className = 'search-item';
                            div.innerText = model;
                            div.onclick = function() {
                                selectModel(model);
                            };
                            resultsContainer.appendChild(div);
                        });

                        resultsContainer.classList.remove('hidden');
                    }

                    function selectModel(modelName) {
                        document.getElementById('modelSearchInput').value = modelName;
                        document.getElementById('searchResults').classList.add('hidden');
                        selectedIemName = modelName;
                        updateSquigGraph();
                    }

                    function syncPreamp(val) {
                        currentPreamp = parseFloat(val);
                        document.getElementById('preampSlider').value = currentPreamp;
                        document.getElementById('preampVal').innerText = (currentPreamp > 0 ? "+" : "") + currentPreamp.toFixed(1) + " dB";
                        drawSquigGraph();
                    }

                    function updateSquigGraph() {
                        document.getElementById('labelIem').innerText = selectedIemName;

                        const targetKey = document.getElementById('targetSelect').value;
                        const targetVals = targets[targetKey] || targets.harman2019;
                        const maxGainNeeded = Math.max(...targetVals);
                        
                        const computedPreamp = maxGainNeeded > 0 ? -maxGainNeeded : 0.0;
                        syncPreamp(computedPreamp);
                    }

                    function drawSquigGraph() {
                        const canvas = document.getElementById('squigCanvas');
                        const ctx = canvas.getContext('2d');
                        canvas.width = canvas.offsetWidth;
                        canvas.height = canvas.offsetHeight;

                        const w = canvas.width;
                        const h = canvas.height;

                        ctx.clearRect(0, 0, w, h);

                        function logX(freq) {
                            return ((Math.log10(freq) - Math.log10(20)) / (Math.log10(20000) - Math.log10(20))) * w;
                        }

                        function valY(val) {
                            return h - (((val - (-15)) / (25 - (-15))) * h);
                        }

                        const targetKey = document.getElementById('targetSelect').value;
                        const targetVals = targets[targetKey] || targets.harman2019;

                        ctx.strokeStyle = '#ef4444';
                        ctx.lineWidth = 2;
                        ctx.beginPath();
                        isoFreqs.forEach((f, i) => {
                            const x = logX(f);
                            const y = valY(targetVals[i]);
                            if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
                        });
                        ctx.stroke();

                        ctx.strokeStyle = '#3b82f6';
                        ctx.lineWidth = 2;
                        ctx.beginPath();
                        isoFreqs.forEach((f, i) => {
                            const x = logX(f);
                            const iemVal = targetVals[i] * 0.4 + (i % 2 === 0 ? 2 : -2);
                            const y = valY(iemVal);
                            if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
                        });
                        ctx.stroke();
                    }

                    function saveProfile() {
                        const name = document.getElementById('profileName').value || selectedIemName + " Profile";
                        const targetKey = document.getElementById('targetSelect').value;

                        profiles.push({
                            name: name,
                            iem: selectedIemName,
                            target: targetKey,
                            preamp: currentPreamp
                        });

                        closeProfileModal();
                        renderProfileList();
                    }

                    function renderProfileList() {
                        const container = document.getElementById('profileList');
                        const initialView = document.getElementById('initialView');

                        if (profiles.length === 0) {
                            initialView.classList.remove('hidden');
                            container.classList.add('hidden');
                            return;
                        }

                        initialView.classList.add('hidden');
                        container.classList.remove('hidden');
                        container.innerHTML = '';

                        profiles.forEach((p, i) => {
                            const card = document.createElement('div');
                            card.className = 'profile-card';
                            card.innerHTML = 
                                '<div>' +
                                    '<div class="profile-name">' + p.name + '</div>' +
                                    '<div class="profile-sub">Model: ' + p.iem + ' | Target: ' + p.target.toUpperCase() + ' | Preamp: ' + p.preamp.toFixed(1) + ' dB</div>' +
                                '</div>' +
                                '<button class="btn-add-main" style="padding: 8px 16px; font-size: 12px;" onclick="openProfileModal()">Edit</button>';
                            container.appendChild(card);
                        });
                    }

                    function openSettings() {
                        alert("Settings: Audio routing and global options.");
                    }
                </script>
            </body>
            </html>
            """.trimIndent()

            val bytes = html.toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            val os: OutputStream = exchange.responseBody
            os.write(bytes)
            os.close()
        }
    }
}
