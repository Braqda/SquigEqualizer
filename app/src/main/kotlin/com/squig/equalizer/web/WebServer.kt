package com.squig.equalizer.web

import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpExchange
import java.net.InetSocketAddress
import java.io.OutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executors
import android.content.Context
import android.content.Intent
import com.squig.equalizer.service.AudioEqualizerService

class WebServer(private val context: Context? = null, private val port: Int = 8080) {

    fun start() {
        val server = HttpServer.create(InetSocketAddress(port), 0)
        server.createContext("/", DashboardHandler())
        server.createContext("/api/apply-eq", ApplyEqHandler(context))
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
                <title>Magnitude response</title>
                <style>
                    :root {
                        --bg-main: #f6f0f8;
                        --bg-card: #fcf8fd;
                        --bg-input: #eee7f2;
                        --accent: #5e399b;
                        --text-main: #1d1a22;
                        --text-dim: #79747e;
                        --border: #e8e0ec;
                        --grid-line: #ded7e3;
                    }
                    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
                    body { background: var(--bg-main); color: var(--text-main); min-height: 100vh; padding: 12px; }

                    .top-nav { display: flex; align-items: center; gap: 12px; padding: 8px 4px 16px 4px; }
                    .back-btn { font-size: 22px; cursor: pointer; border: none; background: transparent; color: var(--text-main); }
                    .page-title { font-size: 20px; font-weight: 500; }

                    .toolbar { display: flex; gap: 8px; margin-bottom: 16px; overflow-x: auto; }
                    .tool-btn {
                        background: var(--bg-input); color: var(--accent); border: none;
                        padding: 10px 16px; border-radius: 12px; font-size: 13px; font-weight: 600;
                        cursor: pointer; display: flex; align-items: center; gap: 8px; flex: 1; justify-content: center;
                        white-space: nowrap;
                    }

                    .card {
                        background: var(--bg-card); border-radius: 20px; padding: 18px;
                        margin-bottom: 16px; border: 1px solid var(--border);
                    }
                    .card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
                    .card-title { font-size: 16px; font-weight: 600; color: var(--text-main); }

                    .profile-item {
                        display: flex; justify-content: space-between; align-items: center;
                        padding: 12px 0; border-bottom: 1px solid var(--border);
                    }
                    .profile-item:last-child { border-bottom: none; }
                    .profile-info { flex: 1; cursor: pointer; }
                    .profile-name { font-weight: 600; font-size: 15px; }
                    .profile-sub { font-size: 12px; color: var(--text-dim); margin-top: 2px; }

                    .switch { position: relative; display: inline-block; width: 48px; height: 26px; }
                    .switch input { opacity: 0; width: 0; height: 0; }
                    .slider {
                        position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0;
                        background-color: #d1c5d8; transition: .3s; border-radius: 26px;
                    }
                    .slider:before {
                        position: absolute; content: ""; height: 20px; width: 20px; left: 3px; bottom: 3px;
                        background-color: white; transition: .3s; border-radius: 50%;
                    }
                    input:checked + .slider { background-color: var(--accent); }
                    input:checked + .slider:before { transform: translateX(22px); }

                    .node-box-empty {
                        min-height: 140px; display: flex; flex-direction: column;
                        align-items: center; justify-content: center; color: var(--text-dim); font-size: 14px;
                    }
                    .node-dots-icon { font-size: 24px; letter-spacing: 2px; color: var(--text-dim); margin-bottom: 6px; }

                    .node-grid-list {
                        display: grid; grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
                        gap: 8px; margin-top: 10px; max-height: 180px; overflow-y: auto;
                    }
                    .node-chip {
                        background: var(--bg-input); border-radius: 8px; padding: 6px 10px;
                        font-size: 12px; font-weight: 500; display: flex; justify-content: space-between;
                    }

                    .graph-container { position: relative; width: 100%; height: 180px; margin-top: 10px; }
                    canvas { width: 100%; height: 100%; border-radius: 8px; display: block; }

                    .eq-grid { display: flex; gap: 6px; overflow-x: auto; padding: 12px 0; margin-top: 12px; }
                    .eq-col { display: flex; flex-direction: column; align-items: center; min-width: 28px; }
                    .eq-val { font-size: 9px; color: var(--text-dim); margin-bottom: 4px; }
                    .eq-slider {
                        writing-mode: bt-lr; -webkit-appearance: slider-vertical;
                        width: 14px; height: 80px;
                    }
                    .eq-freq { font-size: 8px; color: var(--text-dim); margin-top: 4px; }

                    .post-gain-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
                    .post-gain-slider { flex: 1; -webkit-appearance: none; height: 4px; background: #dcd3e2; border-radius: 2px; }
                    .post-gain-slider::-webkit-slider-thumb {
                        -webkit-appearance: none; width: 20px; height: 20px; border-radius: 50%;
                        background: var(--accent); cursor: pointer;
                    }

                    .modal-backdrop {
                        position: fixed; top: 0; left: 0; right: 0; bottom: 0;
                        background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center;
                        padding: 16px; z-index: 1000;
                    }
                    .modal {
                        background: var(--bg-card); border-radius: 20px; padding: 20px;
                        width: 100%; max-width: 520px; border: 1px solid var(--border);
                    }
                    .modal-title { font-size: 18px; font-weight: 600; margin-bottom: 14px; }
                    select, input[type="text"], textarea {
                        width: 100%; padding: 12px; border-radius: 12px; border: 1px solid var(--border);
                        background: var(--bg-input); font-size: 13px; margin-bottom: 12px; color: var(--text-main);
                    }
                    textarea { height: 100px; font-family: monospace; }
                    .btn-primary { background: var(--accent); color: #fff; border: none; padding: 12px 20px; border-radius: 12px; font-weight: 600; cursor: pointer; }
                    .btn-secondary { background: var(--bg-input); color: var(--text-main); border: none; padding: 12px 20px; border-radius: 12px; font-weight: 600; cursor: pointer; margin-right: 8px; }

                    .hidden { display: none !important; }
                </style>
            </head>
            <body>

                <div id="screenProfiles">
                    <div class="top-nav">
                        <span class="page-title">SquigEqualizer</span>
                    </div>

                    <div class="card">
                        <div class="card-header">
                            <span class="card-title">Profiles (<span id="profileCount">0</span>/5)</span>
                            <button class="tool-btn" style="flex:0; padding:6px 16px;" onclick="openMagnitudeEditor(-1)">+ Add Profile</button>
                        </div>
                        <div id="profilesList"></div>
                    </div>
                </div>

                <div id="screenMagnitude" class="hidden">
                    <div class="top-nav">
                        <button class="back-btn" onclick="showProfilesScreen()">←</button>
                        <span class="page-title">Magnitude response</span>
                    </div>

                    <div class="toolbar">
                        <button class="tool-btn" onclick="resetCurrentProfile()">🗑 Reset</button>
                        <button class="tool-btn" onclick="openAutoEqModal()">📥 AutoEQ profiles</button>
                        <button class="tool-btn" onclick="openStringModal()">✏️ Edit as string</button>
                    </div>

                    <div class="card">
                        <span class="card-title">Node list</span>
                        <div id="nodeListContainer">
                            <div class="node-box-empty">
                                <div class="node-dots-icon">◦—◦</div>
                                <span>No nodes defined</span>
                            </div>
                        </div>
                    </div>

                    <div class="card">
                        <span class="card-title">Preview</span>
                        <div class="graph-container">
                            <canvas id="previewCanvas"></canvas>
                        </div>
                        <div id="eqSlidersGrid" class="eq-grid"></div>
                    </div>

                    <div class="card">
                        <span class="card-title" style="display:block; margin-bottom:12px;">Post gain</span>
                        <div class="post-gain-row">
                            <input type="range" id="postGainSlider" class="post-gain-slider" min="-20" max="10" step="0.25" value="0" oninput="updatePostGain(this.value)">
                            <span id="postGainVal" style="font-weight:600; font-size:15px; min-width:65px; text-align:right;">0.00dB</span>
                        </div>
                    </div>

                    <div style="display:flex; justify-content:flex-end; margin-top:12px;">
                        <button class="btn-primary" style="width:100%; padding:14px; font-size:15px;" onclick="saveMagnitudeProfile()">Save Profile</button>
                    </div>
                </div>

                <div id="autoEqModal" class="modal-backdrop hidden">
                    <div class="modal">
                        <div class="modal-title">AutoEQ Import</div>
                        
                        <label style="font-size:11px; color:var(--text-dim); text-transform:uppercase;">Select IEM Model</label>
                        <select id="autoEqIem" onchange="updateAutoEqCurves()">
                            <option value="KZ Castor Harman (0000)">KZ Castor Harman (0000 - Standard)</option>
                            <option value="KZ Castor Harman (1000)">KZ Castor Harman (1000 - Sub-Bass Boost)</option>
                            <option value="KZ Castor Harman (1111)">KZ Castor Harman (1111 - All Switches On)</option>
                            <option value="KZ Castor Bass (1100)">KZ Castor Bass (1100 - Max Bass)</option>
                            <option value="7Hz Salnotes Zero">7Hz Salnotes Zero</option>
                            <option value="Moondrop Chu II">Moondrop Chu II</option>
                        </select>

                        <label style="font-size:11px; color:var(--text-dim); text-transform:uppercase;">Target Curve</label>
                        <select id="autoEqTarget" onchange="updateAutoEqCurves()">
                            <option value="Harman 2019 IE">Harman 2019 IE Target</option>
                            <option value="IEF Neutral">IEF Neutral Target</option>
                            <option value="JM-1 Target">JM-1 Target</option>
                        </select>

                        <label style="font-size:11px; color:var(--text-dim); text-transform:uppercase;">Response Curves (Stock IEM vs Target)</label>
                        <div style="width:100%; height:130px; margin-bottom:12px;">
                            <canvas id="autoEqCanvas"></canvas>
                        </div>

                        <div style="display:flex; justify-content:flex-end;">
                            <button class="btn-secondary" onclick="closeModal('autoEqModal')">Cancel</button>
                            <button class="btn-primary" onclick="importAutoEqValues()">Import Values</button>
                        </div>
                    </div>
                </div>

                <div id="stringModal" class="modal-backdrop hidden">
                    <div class="modal">
                        <div class="modal-title">Edit as string</div>
                        <textarea id="stringInput" placeholder="GraphicEQ: 20 0; 25 -1.5; 31.5 -2.0; 40 -1.0; 50 0; 63 1.5; ..."></textarea>
                        <div style="display:flex; justify-content:flex-end;">
                            <button class="btn-secondary" onclick="closeModal('stringModal')">Cancel</button>
                            <button class="btn-primary" onclick="applyStringEq()">Apply String</button>
                        </div>
                    </div>
                </div>

                <script>
                    const isoFreqs = [20, 25, 31.5, 40, 50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630, 800, 1000, 1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000, 12500, 16000, 18000, 20000];
                    
                    let profiles = [];
                    let editingProfileIndex = -1;

                    let currentBands = new Array(32).fill(0.0);
                    let currentPostGain = 0.0;
                    let currentIemName = "Generic Target";
                    let currentNodes = [];

                    const iemCurves = {
                        "KZ Castor Harman (0000)": [6, 6, 5.5, 5, 4, 3, 2, 1, 0, 0, 0, 0, 0, 0, 1, 2, 4, 7, 9, 7, 4, 2, 3, 5, 4, 2, 0, -2, -4, -6, -8, -10],
                        "KZ Castor Harman (1000)": [9, 9, 8.5, 8, 6.5, 5, 3, 1.5, 0, 0, 0, 0, 0, 0, 1, 2, 4, 7, 9, 7, 4, 2, 3, 5, 4, 2, 0, -2, -4, -6, -8, -10],
                        "KZ Castor Harman (1111)": [10, 10, 9.5, 9, 7.5, 6, 4, 2, 1, 0, 0, 0, 0, 0, 2, 3, 5, 8, 10, 8, 5, 3, 4, 6, 5, 3, 1, -1, -3, -5, -7, -9],
                        "KZ Castor Bass (1100)": [12, 12, 11, 10, 8, 6, 4, 2, 0, 0, 0, 0, 0, 0, 1, 2, 3, 6, 8, 6, 3, 1, 2, 4, 3, 1, -1, -3, -5, -7, -9, -11],
                        "7Hz Salnotes Zero": [4, 4, 3.5, 3, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 4, 6, 8, 6, 3, 1, 2, 4, 3, 1, -1, -3, -5, -7, -9, -11],
                        "Moondrop Chu II": [7, 7, 6.5, 6, 4.5, 3, 1.5, 0, 0, 0, 0, 0, 0, 0, 1, 2, 4, 7, 9, 7, 4, 2, 3, 5, 4, 2, 0, -2, -4, -6, -8, -10]
                    };

                    const targetCurves = {
                        "Harman 2019 IE": [9, 9, 8, 6.5, 5, 3.5, 2, 1, 0, 0, 0, 0, 0, 0, 1, 2, 4, 7, 9, 8, 5, 3, 4, 6, 5, 3, 1, -1, -3, -5, -7, -9],
                        "IEF Neutral": [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 4, 7, 8, 6, 3, 1, 2, 4, 3, 1, -1, -3, -5, -7, -9, -11],
                        "JM-1 Target": [6, 6, 5.5, 4.5, 3.5, 2.5, 1.5, 0.5, 0, 0, 0, 0, 0, 0, 1, 2, 4, 7, 8.5, 7, 4, 2, 3, 5, 4, 2, 0, -2, -4, -6, -8, -10]
                    };

                    function renderProfilesList() {
                        const container = document.getElementById('profilesList');
                        document.getElementById('profileCount').innerText = profiles.length;
                        container.innerHTML = '';
                        if(profiles.length === 0) {
                            container.innerHTML = '<div style="font-size:13px; color:var(--text-dim); text-align:center; padding:16px;">No profiles added yet</div>';
                            return;
                        }
                        profiles.forEach((p, i) => {
                            const item = document.createElement('div');
                            item.className = 'profile-item';
                            item.innerHTML = 
                                '<div class="profile-info" onclick="openMagnitudeEditor(' + i + ')">' +
                                    '<div class="profile-name">' + p.name + '</div>' +
                                    '<div class="profile-sub">' + p.iem + ' | Post gain: ' + p.postGain.toFixed(2) + 'dB</div>' +
                                '</div>' +
                                '<label class="switch">' +
                                    '<input type="checkbox" ' + (p.active ? 'checked' : '') + ' onchange="toggleProfileActive(' + i + ')">' +
                                    '<span class="slider"></span>' +
                                '</label>';
                            container.appendChild(item);
                        });
                    }

                    function toggleProfileActive(idx) {
                        const newState = !profiles[idx].active;
                        profiles.forEach(p => p.active = false);
                        profiles[idx].active = newState;
                        renderProfilesList();
                        syncToAndroidService();
                    }

                    function showProfilesScreen() {
                        document.getElementById('screenMagnitude').classList.add('hidden');
                        document.getElementById('screenProfiles').classList.remove('hidden');
                    }

                    function openMagnitudeEditor(index) {
                        editingProfileIndex = index;
                        if (index >= 0) {
                            const p = profiles[index];
                            currentBands = [...p.bands];
                            currentPostGain = p.postGain;
                            currentIemName = p.iem;
                            currentNodes = p.nodes ? [...p.nodes] : [];
                        } else {
                            if (profiles.length >= 5) {
                                alert("Maximum limit of 5 profiles reached.");
                                return;
                            }
                            currentBands = new Array(32).fill(0.0);
                            currentPostGain = 0.0;
                            currentIemName = "New Target Profile";
                            currentNodes = [];
                        }

                        document.getElementById('postGainSlider').value = currentPostGain;
                        document.getElementById('postGainVal').innerText = currentPostGain.toFixed(2) + "dB";

                        document.getElementById('screenProfiles').classList.add('hidden');
                        document.getElementById('screenMagnitude').classList.remove('hidden');
                        renderNodeList();
                        renderMagnitudeEditor();
                    }

                    function renderNodeList() {
                        const container = document.getElementById('nodeListContainer');
                        if (currentNodes.length === 0) {
                            container.innerHTML = 
                                '<div class="node-box-empty">' +
                                    '<div class="node-dots-icon">◦—◦</div>' +
                                    '<span>No nodes defined</span>' +
                                '</div>';
                            return;
                        }

                        let html = '<div class="node-grid-list">';
                        currentNodes.forEach(node => {
                            const freqLabel = node.freq >= 1000 ? (node.freq/1000).toFixed(1) + 'k' : node.freq;
                            html += '<div class="node-chip"><span>' + freqLabel + ' Hz</span><span style="color:var(--accent);">' + node.gain.toFixed(1) + 'dB</span></div>';
                        });
                        html += '</div>';
                        container.innerHTML = html;
                    }

                    function renderMagnitudeEditor() {
                        const grid = document.getElementById('eqSlidersGrid');
                        grid.innerHTML = '';
                        isoFreqs.forEach((f, i) => {
                            const col = document.createElement('div');
                            col.className = 'eq-col';
                            const label = f >= 1000 ? (f/1000) + 'k' : f;
                            col.innerHTML = 
                                '<span class="eq-val">' + currentBands[i].toFixed(1) + '</span>' +
                                '<input type="range" class="eq-slider" min="-12" max="12" step="0.5" value="' + currentBands[i] + '" oninput="updateBand(' + i + ', this.value)">' +
                                '<span class="eq-freq">' + label + '</span>';
                            grid.appendChild(col);
                        });
                        drawPreviewGraph();
                    }

                    function updateBand(idx, val) {
                        currentBands[idx] = parseFloat(val);
                        currentNodes = [];
                        isoFreqs.forEach((f, i) => {
                            if (currentBands[i] !== 0) {
                                currentNodes.push({ freq: f, gain: currentBands[i] });
                            }
                        });
                        renderNodeList();
                        renderMagnitudeEditor();
                        syncToAndroidService();
                    }

                    function updatePostGain(val) {
                        currentPostGain = parseFloat(val);
                        document.getElementById('postGainVal').innerText = currentPostGain.toFixed(2) + "dB";
                        syncToAndroidService();
                    }

                    function drawPreviewGraph() {
                        const canvas = document.getElementById('previewCanvas');
                        const ctx = canvas.getContext('2d');
                        canvas.width = canvas.offsetWidth;
                        canvas.height = canvas.offsetHeight;
                        const w = canvas.width, h = canvas.height;
                        ctx.clearRect(0, 0, w, h);

                        ctx.strokeStyle = '#ded7e3';
                        ctx.lineWidth = 1;
                        for(let y=20; y<h; y+=30) {
                            ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(w, y); ctx.stroke();
                        }

                        ctx.strokeStyle = '#5e399b';
                        ctx.lineWidth = 2.5;
                        ctx.beginPath();
                        isoFreqs.forEach((f, i) => {
                            const x = ((Math.log10(f) - Math.log10(20)) / (Math.log10(20000) - Math.log10(20))) * w;
                            const y = h/2 - (currentBands[i] / 12) * (h/2 - 15);
                            if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
                        });
                        ctx.stroke();
                    }

                    function resetCurrentProfile() {
                        currentBands.fill(0.0);
                        currentPostGain = 0.0;
                        currentNodes = [];
                        document.getElementById('postGainSlider').value = 0;
                        document.getElementById('postGainVal').innerText = "0.00dB";
                        renderNodeList();
                        renderMagnitudeEditor();
                        syncToAndroidService();
                    }

                    function openAutoEqModal() {
                        document.getElementById('autoEqModal').classList.remove('hidden');
                        updateAutoEqCurves();
                    }

                    function closeModal(id) { document.getElementById(id).classList.add('hidden'); }

                    function updateAutoEqCurves() {
                        const iemKey = document.getElementById('autoEqIem').value;
                        const targetKey = document.getElementById('autoEqTarget').value;
                        const iemData = iemCurves[iemKey] || iemCurves["KZ Castor Harman (0000)"];
                        const targetData = targetCurves[targetKey] || targetCurves["Harman 2019 IE"];

                        const canvas = document.getElementById('autoEqCanvas');
                        const ctx = canvas.getContext('2d');
                        canvas.width = canvas.offsetWidth;
                        canvas.height = canvas.offsetHeight;
                        const w = canvas.width, h = canvas.height;
                        ctx.clearRect(0, 0, w, h);

                        ctx.strokeStyle = '#3b82f6';
                        ctx.lineWidth = 2;
                        ctx.beginPath();
                        isoFreqs.forEach((f, i) => {
                            const x = ((Math.log10(f) - Math.log10(20)) / (Math.log10(20000) - Math.log10(20))) * w;
                            const y = h/2 - (iemData[i] / 15) * (h/2 - 10);
                            if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
                        });
                        ctx.stroke();

                        ctx.strokeStyle = '#ef4444';
                        ctx.lineWidth = 2;
                        ctx.beginPath();
                        isoFreqs.forEach((f, i) => {
                            const x = ((Math.log10(f) - Math.log10(20)) / (Math.log10(20000) - Math.log10(20))) * w;
                            const y = h/2 - (targetData[i] / 15) * (h/2 - 10);
                            if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
                        });
                        ctx.stroke();
                    }

                    function importAutoEqValues() {
                        const iemKey = document.getElementById('autoEqIem').value;
                        const targetKey = document.getElementById('autoEqTarget').value;
                        const iemData = iemCurves[iemKey] || iemCurves["KZ Castor Harman (0000)"];
                        const targetData = targetCurves[targetKey] || targetCurves["Harman 2019 IE"];

                        currentIemName = iemKey;
                        currentNodes = [];
                        isoFreqs.forEach((f, i) => {
                            const delta = targetData[i] - iemData[i];
                            currentBands[i] = Math.min(Math.max(delta, -12), 12);
                            if (Math.abs(currentBands[i]) > 0.1) {
                                currentNodes.push({ freq: f, gain: currentBands[i] });
                            }
                        });

                        closeModal('autoEqModal');
                        renderNodeList();
                        renderMagnitudeEditor();
                        syncToAndroidService();
                    }

                    function openStringModal() { document.getElementById('stringModal').classList.remove('hidden'); }

                    function applyStringEq() {
                        const str = document.getElementById('stringInput').value;
                        if(str.includes("GraphicEQ:")) {
                            currentBands.fill(0.0);
                            currentNodes = [];
                            const pairs = str.replace("GraphicEQ:", "").split(";");
                            pairs.forEach(pair => {
                                const parts = pair.trim().split(" ");
                                if(parts.length === 2) {
                                    const f = parseFloat(parts[0]);
                                    const g = parseFloat(parts[1]);
                                    const closestIdx = isoFreqs.reduce((best, curr, i) => Math.abs(curr - f) < Math.abs(isoFreqs[best] - f) ? i : best, 0);
                                    currentBands[closestIdx] = g;
                                    currentNodes.push({ freq: isoFreqs[closestIdx], gain: g });
                                }
                            });
                        }
                        closeModal('stringModal');
                        renderNodeList();
                        renderMagnitudeEditor();
                        syncToAndroidService();
                    }

                    function saveMagnitudeProfile() {
                        if (editingProfileIndex >= 0) {
                            profiles[editingProfileIndex].bands = [...currentBands];
                            profiles[editingProfileIndex].postGain = currentPostGain;
                            profiles[editingProfileIndex].iem = currentIemName;
                            profiles[editingProfileIndex].nodes = [...currentNodes];
                        } else {
                            profiles.forEach(p => p.active = false);
                            profiles.push({
                                name: currentIemName,
                                iem: currentIemName,
                                bands: [...currentBands],
                                postGain: currentPostGain,
                                nodes: [...currentNodes],
                                active: true
                            });
                        }
                        showProfilesScreen();
                        renderProfilesList();
                        syncToAndroidService();
                    }

                    async function syncToAndroidService() {
                        const activeProfile = profiles.find(p => p.active);
                        const gainsToSend = activeProfile ? activeProfile.bands : currentBands;
                        const postGainToSend = activeProfile ? activeProfile.postGain : currentPostGain;

                        try {
                            await fetch('/api/apply-eq', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify({
                                    active: true,
                                    postGain: postGainToSend,
                                    gains: gainsToSend
                                })
                            });
                        } catch(e) {}
                    }

                    renderProfilesList();
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

    class ApplyEqHandler(private val context: Context?) : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            if (exchange.requestMethod.equals("POST", ignoreCase = true)) {
                val reader = BufferedReader(InputStreamReader(exchange.requestBody))
                val body = reader.readText()
                reader.close()

                println("\n>>> [API RECEIVED FROM WEB DASHBOARD] <<<")
                println("Payload: $body\n")

                if (context != null) {
                    val intent = Intent(context, AudioEqualizerService::class.java)
                    intent.putExtra("AUDIO_SESSION_ID", 0)
                    intent.putExtra("PREAMP_GAIN", -3.0f)
                    context.startService(intent)
                }

                val response = "{\"status\":\"success\"}"
                exchange.responseHeaders.set("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                exchange.responseBody.write(response.toByteArray())
                exchange.responseBody.close()
            } else {
                exchange.sendResponseHeaders(405, 0)
                exchange.responseBody.close()
            }
        }
    }
}
