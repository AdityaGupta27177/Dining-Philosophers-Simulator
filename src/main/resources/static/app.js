/**
 * Dining Philosophers Simulator - Bright & Luminous Luxury Client Application
 * Apple / Stripe Inspired High-End Concurrency Visualizer
 */

document.addEventListener('DOMContentLoaded', () => {
  let currentState = null;
  let sseSource = null;
  let mealsChart = null;
  let benchmarkMealsChart = null;
  let benchmarkWaitChart = null;
  let fallbackPollTimer = null;
  let previousDeadlockState = false;
  let previousEatingStates = new Map();

  let demoActive = false;
  let demoStep = 1;
  let demoTimer = null;
  let demoPaused = false;

  const el = {
    statusPill: document.getElementById('status-pill'),
    statusDot: document.getElementById('status-dot'),
    statusText: document.getElementById('status-text'),
    ssePill: document.getElementById('sse-pill'),
    sseDot: document.getElementById('sse-dot'),
    sseText: document.getElementById('sse-text'),

    deadlockAlert: document.getElementById('deadlock-alert'),
    deadlockPathLabel: document.getElementById('deadlock-path-label'),
    btnBannerRecover: document.getElementById('btn-banner-recover'),

    heroSection: document.getElementById('hero-section'),
    svgTable: document.getElementById('svg-dining-table'),
    tableRim: document.getElementById('table-rim'),
    layerWfg: document.getElementById('layer-wfg'),
    layerForks: document.getElementById('layer-forks'),
    layerPhilosophers: document.getElementById('layer-philosophers'),
    footerStrategyName: document.getElementById('footer-strategy-name'),
    footerBrokenCondition: document.getElementById('footer-broken-condition'),

    selectStrategy: document.getElementById('select-strategy'),
    strategyDescText: document.getElementById('strategy-desc-text'),
    labelConditionBroken: document.getElementById('label-condition-broken'),
    sliderPhilosophers: document.getElementById('slider-philosophers'),
    valPhilosophers: document.getElementById('val-philosophers'),
    sliderThink: document.getElementById('slider-think'),
    valThink: document.getElementById('val-think'),
    sliderEat: document.getElementById('slider-eat'),
    valEat: document.getElementById('val-eat'),

    btnStart: document.getElementById('btn-start'),
    btnStop: document.getElementById('btn-stop'),
    btnReset: document.getElementById('btn-reset'),
    btnRecover: document.getElementById('btn-recover'),
    btnDemoMode: document.getElementById('btn-demo-mode'),

    statMeals: document.getElementById('stat-meals'),
    statDeadlocks: document.getElementById('stat-deadlocks'),
    statWait: document.getElementById('stat-wait'),
    statFairness: document.getElementById('stat-fairness'),

    terminalBody: document.getElementById('terminal-body'),
    btnClearTerminal: document.getElementById('btn-clear-terminal'),

    tabButtons: document.querySelectorAll('.nav-tab-btn'),
    tabPanes: document.querySelectorAll('.tab-pane'),

    historyTableBody: document.getElementById('history-table-body'),
    btnRefreshHistory: document.getElementById('btn-refresh-history'),
    btnRunBenchmark: document.getElementById('btn-run-benchmark'),
    benchmarkLoader: document.getElementById('benchmark-loader'),
    benchmarkTableBody: document.getElementById('benchmark-table-body'),

    demoBanner: document.getElementById('demo-banner'),
    demoStepBadge: document.getElementById('demo-step-badge'),
    demoCaptionTitle: document.getElementById('demo-caption-title'),
    demoCaptionDesc: document.getElementById('demo-caption-desc'),
    demoProgressBar: document.getElementById('demo-progress-bar'),
    btnDemoPause: document.getElementById('btn-demo-pause'),
    btnDemoSkip: document.getElementById('btn-demo-skip'),
    btnDemoExit: document.getElementById('btn-demo-exit'),

    toastContainer: document.getElementById('toast-container')
  };

  const strategyMeta = {
    naive: {
      name: "1. Naive Strategy (Deadlock-Prone)",
      condition: "None (All 4 Hold)",
      desc: "Picks left fork first, pauses 100ms, then attempts right fork. Satisfies Mutual Exclusion, Hold & Wait, No-Preemption, and Circular Wait, reliably triggering a deadlock."
    },
    maxfour: {
      name: "2. Max-Four Strategy (Semaphore)",
      condition: "Hold & Wait (Capacity Limit)",
      desc: "Limits table occupancy to (N - 1) using a counting Semaphore. By the Pigeonhole Principle, at least one philosopher is always guaranteed both forks."
    },
    resourceordering: {
      name: "3. Resource Ordering (Havender Hierarchy)",
      condition: "Circular Wait (Total Ordering)",
      desc: "Imposes a global strict linear order on forks (0 < 1 < ... < N-1). Always locks the lower-numbered fork before the higher-numbered fork, mathematically preventing dependency cycles."
    },
    asymmetric: {
      name: "4. Asymmetric Strategy (Dijkstra Parity)",
      condition: "Circular Wait (Symmetry Breaking)",
      desc: "Odd-numbered philosophers pick right fork first; even-numbered philosophers pick left fork first. Adjacent philosophers compete for the same fork first, breaking circular wait symmetry."
    },
    waiter: {
      name: "5. Central Waiter (All-or-Nothing Monitor)",
      condition: "Hold & Wait (Atomic Acquisition)",
      desc: "Central monitor grants both forks atomically or none. A philosopher never holds one fork while waiting for another, completely eliminating Hold-and-Wait."
    }
  };

  initLucide();
  initEventListeners();
  initTabs();
  initCharts();
  initSSE();
  fetchState();

  function initLucide() {
    if (window.lucide) {
      lucide.createIcons();
    }
  }

  function initEventListeners() {
    el.selectStrategy.addEventListener('change', (e) => {
      const meta = strategyMeta[e.target.value] || strategyMeta.naive;
      el.strategyDescText.textContent = meta.desc;
      el.labelConditionBroken.textContent = `Breaks: ${meta.condition}`;
      el.footerStrategyName.textContent = meta.name;
      el.footerBrokenCondition.textContent = meta.condition;
    });

    el.sliderPhilosophers.addEventListener('input', (e) => {
      el.valPhilosophers.textContent = `${e.target.value} Diners`;
    });
    el.sliderThink.addEventListener('input', (e) => {
      el.valThink.textContent = `${e.target.value}ms`;
    });
    el.sliderEat.addEventListener('input', (e) => {
      el.valEat.textContent = `${e.target.value}ms`;
    });

    el.btnStart.addEventListener('click', () => startSimulation());
    el.btnStop.addEventListener('click', () => stopSimulation());
    el.btnReset.addEventListener('click', () => resetSimulation());
    el.btnRecover.addEventListener('click', () => recoverDeadlock());
    el.btnBannerRecover.addEventListener('click', () => recoverDeadlock());

    el.btnClearTerminal.addEventListener('click', () => {
      el.terminalBody.innerHTML = '<div class="text-slate-500">[System] Terminal log cleared.</div>';
    });

    el.btnRefreshHistory.addEventListener('click', fetchHistory);
    el.btnRunBenchmark.addEventListener('click', runBenchmark);

    el.btnDemoMode.addEventListener('click', startDemoMode);
    el.btnDemoExit.addEventListener('click', stopDemoMode);
    el.btnDemoPause.addEventListener('click', toggleDemoPause);
    el.btnDemoSkip.addEventListener('click', advanceDemoStep);

    window.addEventListener('keydown', (e) => {
      if (e.target.tagName === 'INPUT' || e.target.tagName === 'SELECT') return;
      if (e.code === 'Space') {
        e.preventDefault();
        if (currentState && currentState.running) {
          stopSimulation();
        } else {
          startSimulation();
        }
      } else if (e.code === 'KeyR') {
        e.preventDefault();
        resetSimulation();
      }
    });
  }

  function initTabs() {
    el.tabButtons.forEach(btn => {
      btn.addEventListener('click', () => {
        const targetId = btn.getAttribute('data-tab');

        el.tabButtons.forEach(b => {
          b.className = 'nav-tab-btn px-4 py-2 rounded-lg text-xs md:text-sm font-semibold text-slate-600 hover:text-slate-900 hover:bg-slate-100 flex items-center gap-2 transition';
        });
        el.tabPanes.forEach(p => p.classList.add('hidden'));

        btn.className = 'nav-tab-btn active px-4 py-2 rounded-lg text-xs md:text-sm font-bold flex items-center gap-2 transition bg-blue-50 text-blue-700 border border-blue-200 shadow-sm';

        const activePane = document.getElementById(targetId);
        if (activePane) {
          activePane.classList.remove('hidden');
          gsap.fromTo(activePane, { opacity: 0, y: 6 }, { opacity: 1, y: 0, duration: 0.25 });
        }

        if (targetId === 'tab-history') {
          fetchHistory();
        }

        initLucide();
      });
    });
  }

  function showToast(title, message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast-item ${type}`;

    let icon = 'info';
    let iconColor = 'text-sky-600';
    if (type === 'success') { icon = 'check-circle'; iconColor = 'text-emerald-600'; }
    else if (type === 'warning') { icon = 'alert-triangle'; iconColor = 'text-amber-600'; }
    else if (type === 'danger') { icon = 'shield-alert'; iconColor = 'text-red-600'; }

    toast.innerHTML = `
      <div class="mt-0.5 ${iconColor} shrink-0">
        <i data-lucide="${icon}" class="w-5 h-5"></i>
      </div>
      <div class="flex-1">
        <div class="font-extrabold text-xs text-slate-900 uppercase tracking-wider">${escapeHtml(title)}</div>
        <div class="text-xs text-slate-600 mt-0.5 leading-snug">${escapeHtml(message)}</div>
      </div>
    `;

    el.toastContainer.appendChild(toast);
    initLucide();

    setTimeout(() => {
      gsap.to(toast, {
        opacity: 0,
        x: 40,
        duration: 0.3,
        onComplete: () => toast.remove()
      });
    }, 4000);
  }

  async function fetchState() {
    try {
      const resp = await fetch('/api/state');
      if (resp.ok) {
        const state = await resp.json();
        updateUI(state);
      }
    } catch (e) {
      console.warn('Initial state fetch error:', e);
    }
  }

  async function startSimulation(overrideParams = null) {
    const payload = overrideParams || {
      strategy: el.selectStrategy.value,
      philosophers: parseInt(el.sliderPhilosophers.value, 10),
      thinkMs: parseInt(el.sliderThink.value, 10),
      eatMs: parseInt(el.sliderEat.value, 10)
    };

    try {
      el.btnStart.disabled = true;
      const resp = await fetch('/api/start', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      if (resp.ok) {
        const state = await resp.json();
        updateUI(state);
        showToast('Simulation Started', `Engine running with ${payload.philosophers} philosophers using ${state.strategyName}.`, 'success');
      }
    } catch (e) {
      showToast('Error', 'Failed to start simulation', 'danger');
    }
  }

  async function stopSimulation() {
    try {
      el.btnStop.disabled = true;
      const resp = await fetch('/api/stop', { method: 'POST' });
      if (resp.ok) {
        const state = await resp.json();
        updateUI(state);
        showToast('Simulation Paused', 'Worker threads stopped and state persisted to H2.', 'warning');
      }
    } catch (e) {
      showToast('Error', 'Failed to stop simulation', 'danger');
    }
  }

  async function resetSimulation() {
    try {
      const resp = await fetch('/api/reset', { method: 'POST' });
      if (resp.ok) {
        const state = await resp.json();
        previousDeadlockState = false;
        updateUI(state);
        showToast('System Reset', 'All forks and philosopher telemetry cleared.', 'info');
      }
    } catch (e) {
      showToast('Error', 'Failed to reset simulation', 'danger');
    }
  }

  async function recoverDeadlock() {
    try {
      el.btnRecover.disabled = true;
      el.btnBannerRecover.disabled = true;

      const resp = await fetch('/api/recover', { method: 'POST' });
      if (resp.ok) {
        const data = await resp.json();
        showToast('Deadlock Resolved', data.message, 'success');
        if (data.state) {
          updateUI(data.state);
        }
      }
    } catch (e) {
      showToast('Recovery Failed', 'Error executing preemption', 'danger');
    } finally {
      el.btnRecover.disabled = false;
      el.btnBannerRecover.disabled = false;
    }
  }

  async function fetchHistory() {
    try {
      const resp = await fetch('/api/history');
      if (resp.ok) {
        const runs = await resp.json();
        renderHistoryTable(runs);
      }
    } catch (e) {
      console.error('History fetch error:', e);
    }
  }

  async function runBenchmark() {
    try {
      el.btnRunBenchmark.disabled = true;
      el.benchmarkLoader.classList.remove('hidden');

      const resp = await fetch('/api/compare?durationSec=15');
      if (resp.ok) {
        const results = await resp.json();
        renderBenchmarkResults(results);
        showToast('Benchmark Complete', 'Evaluated all 5 strategies under 15s load.', 'success');
      }
    } catch (e) {
      showToast('Benchmark Error', 'Could not complete benchmark test.', 'danger');
    } finally {
      el.btnRunBenchmark.disabled = false;
      el.benchmarkLoader.classList.add('hidden');
    }
  }

  function initSSE() {
    if (sseSource) sseSource.close();

    try {
      sseSource = new EventSource('/api/events');

      sseSource.onopen = () => {
        el.ssePill.className = 'flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs text-emerald-700 border border-emerald-200 bg-emerald-50 font-mono font-semibold';
        el.sseDot.className = 'w-2 h-2 rounded-full bg-emerald-500 shadow-sm shadow-emerald-500';
        el.sseText.textContent = 'SSE Connected';

        if (fallbackPollTimer) {
          clearInterval(fallbackPollTimer);
          fallbackPollTimer = null;
        }
      };

      sseSource.addEventListener('INIT', (e) => {
        try {
          const data = JSON.parse(e.data);
          if (data.state) updateUI(data.state);
          if (data.recentEvents) {
            data.recentEvents.forEach(ev => appendTerminalLog(ev.type, ev.message, ev.timestamp));
          }
        } catch (err) {
          console.error('SSE INIT parsing error:', err);
        }
      });

      sseSource.addEventListener('SIM_EVENT', (e) => {
        try {
          const data = JSON.parse(e.data);
          if (data.event) {
            appendTerminalLog(data.event.type, data.event.message, data.event.timestamp);
          }
          if (data.state) {
            updateUI(data.state);
          }
        } catch (err) {
          console.error('SSE SIM_EVENT parsing error:', err);
        }
      });

      sseSource.onerror = () => {
        el.ssePill.className = 'flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs text-amber-700 border border-amber-200 bg-amber-50 font-mono font-semibold';
        el.sseDot.className = 'w-2 h-2 rounded-full bg-amber-500 animate-ping';
        el.sseText.textContent = 'Reconnecting...';

        if (!fallbackPollTimer) {
          fallbackPollTimer = setInterval(fetchState, 800);
        }
      };
    } catch (e) {
      fallbackPollTimer = setInterval(fetchState, 800);
    }
  }

  function updateUI(state) {
    if (!state) return;
    currentState = state;

    if (state.deadlocked) {
      el.statusPill.className = 'flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-bold tracking-wider uppercase border border-red-300 bg-red-50 text-red-700 shadow-sm';
      el.statusDot.className = 'w-2 h-2 rounded-full bg-red-600 shadow-sm shadow-red-500 animate-ping';
      el.statusText.textContent = 'DEADLOCKED';

      el.deadlockAlert.classList.remove('hidden');
      el.deadlockPathLabel.textContent = state.cyclePath || 'Cycle Detected';
      el.btnRecover.disabled = false;
      el.btnRecover.className = 'btn-glow py-2 px-3 rounded-xl text-xs font-bold text-white btn-recover-glow cursor-pointer flex items-center justify-center gap-1.5 transition';

      if (!previousDeadlockState) {
        previousDeadlockState = true;
        el.heroSection.classList.add('screen-shake');
        setTimeout(() => el.heroSection.classList.remove('screen-shake'), 600);
        showToast('Deadlock Detected!', `Wait-for cycle detected: ${state.cyclePath}`, 'danger');
      }
    } else if (state.running) {
      previousDeadlockState = false;
      el.statusPill.className = 'flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-bold tracking-wider uppercase border border-emerald-200 bg-emerald-50 text-emerald-700 shadow-sm';
      el.statusDot.className = 'w-2 h-2 rounded-full bg-emerald-500 shadow-sm shadow-emerald-500 animate-pulse';
      el.statusText.textContent = 'RUNNING';

      el.deadlockAlert.classList.add('hidden');
      el.btnRecover.disabled = true;
      el.btnRecover.className = 'btn-glow py-2 px-3 rounded-xl text-xs font-bold text-red-700 border border-red-200 bg-red-50 opacity-40 cursor-not-allowed flex items-center justify-center gap-1.5 transition';
    } else {
      previousDeadlockState = false;
      el.statusPill.className = 'flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-bold tracking-wider uppercase border border-slate-200 bg-white text-slate-700 shadow-sm';
      el.statusDot.className = 'w-2 h-2 rounded-full bg-slate-400 animate-pulse';
      el.statusText.textContent = 'IDLE';

      el.deadlockAlert.classList.add('hidden');
      el.btnRecover.disabled = true;
      el.btnRecover.className = 'btn-glow py-2 px-3 rounded-xl text-xs font-bold text-red-700 border border-red-200 bg-red-50 opacity-40 cursor-not-allowed flex items-center justify-center gap-1.5 transition';
    }

    el.btnStart.disabled = state.running;
    if (state.running) {
      el.btnStart.classList.add('opacity-40', 'cursor-not-allowed');
      el.btnStop.disabled = false;
      el.btnStop.classList.remove('opacity-40', 'cursor-not-allowed');
    } else {
      el.btnStart.classList.remove('opacity-40', 'cursor-not-allowed');
      el.btnStop.disabled = true;
      el.btnStop.classList.add('opacity-40', 'cursor-not-allowed');
    }

    el.footerStrategyName.textContent = state.strategyName || 'Naive Strategy';
    el.footerBrokenCondition.textContent = state.brokenCondition || 'None';

    animateCounter(el.statMeals, state.totalMeals || 0);
    animateCounter(el.statDeadlocks, state.deadlocked ? 1 : 0);
    animateCounter(el.statWait, state.maxWaitMs || 0, 'ms');
    animateCounter(el.statFairness, state.fairness !== undefined ? state.fairness : 0);

    renderSvgTable(state);
    updateMealsChart(state);
  }

  function animateCounter(domElement, targetValue, suffix = '') {
    const current = parseFloat(domElement.getAttribute('data-val') || '0');
    if (current === targetValue) return;

    domElement.setAttribute('data-val', targetValue);
    gsap.to({ val: current }, {
      val: targetValue,
      duration: 0.6,
      ease: 'power2.out',
      onUpdate: function () {
        domElement.innerHTML = `${Math.round(this.targets()[0].val)}${suffix ? `<span class="text-xs font-normal text-slate-400">${suffix}</span>` : ''}`;
      }
    });
  }

  function renderSvgTable(state) {
    if (!state || !state.philosophers) return;
    const philosophers = state.philosophers;
    const forks = state.forks || [];
    const n = philosophers.length;
    if (n < 3) return;

    const cx = 300;
    const cy = 300;
    const philRadius = 215;
    const forkRestRadius = 145;
    const forkHeldRadius = 180;

    el.layerWfg.innerHTML = '';
    el.layerForks.innerHTML = '';
    el.layerPhilosophers.innerHTML = '';

    const philPositions = [];
    for (let i = 0; i < n; i++) {
      const angle = (2 * Math.PI * i) / n - Math.PI / 2;
      philPositions.push({
        x: cx + philRadius * Math.cos(angle),
        y: cy + philRadius * Math.sin(angle),
        angle: angle
      });
    }

    // Deadlock cycle loop
    if (state.deadlocked && state.cycleNodes && state.cycleNodes.length > 1) {
      el.tableRim.setAttribute('stroke', '#dc2626');
      el.tableRim.setAttribute('filter', 'url(#glow-deadlock-ring)');

      for (let i = 0; i < state.cycleNodes.length - 1; i++) {
        const u = state.cycleNodes[i];
        const v = state.cycleNodes[i + 1];
        if (u < n && v < n) {
          const from = philPositions[u];
          const to = philPositions[v];

          const midX = (from.x + to.x) / 2;
          const midY = (from.y + to.y) / 2;
          const curveX = (midX + cx) / 2;
          const curveY = (midY + cy) / 2;

          const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
          path.setAttribute('d', `M ${from.x} ${from.y} Q ${curveX} ${curveY} ${to.x} ${to.y}`);
          path.setAttribute('stroke', '#dc2626');
          path.setAttribute('stroke-width', '3.5');
          path.setAttribute('fill', 'none');
          path.setAttribute('class', 'deadlock-arrow-path');
          path.setAttribute('marker-end', 'url(#deadlock-arrow)');
          el.layerWfg.appendChild(path);
        }
      }
    } else {
      el.tableRim.setAttribute('stroke', '#cbd5e1');
      el.tableRim.removeAttribute('filter');
    }

    // Forks
    for (let j = 0; j < forks.length; j++) {
      const fork = forks[j];
      const baseAngle = (2 * Math.PI * (j + 0.5)) / n - Math.PI / 2;
      let fx, fy, rotAngle;
      let strokeColor = '#94a3b8';
      const isHeld = fork.held;
      const ownerId = fork.currentOwnerId;

      if (isHeld && ownerId !== null && ownerId < n) {
        const ownerPos = philPositions[ownerId];
        const heldAngle = (baseAngle + ownerPos.angle) / 2;
        fx = cx + forkHeldRadius * Math.cos(heldAngle);
        fy = cy + forkHeldRadius * Math.sin(heldAngle);
        rotAngle = (heldAngle * 180) / Math.PI + 90;

        const ownerState = philosophers[ownerId].state;
        strokeColor = ownerState === 'EATING' ? '#059669' : '#0284c7';

        const beam = document.createElementNS('http://www.w3.org/2000/svg', 'line');
        beam.setAttribute('x1', fx);
        beam.setAttribute('y1', fy);
        beam.setAttribute('x2', ownerPos.x);
        beam.setAttribute('y2', ownerPos.y);
        beam.setAttribute('stroke', strokeColor);
        beam.setAttribute('stroke-width', '2');
        beam.setAttribute('stroke-dasharray', '3 3');
        beam.setAttribute('opacity', '0.85');
        el.layerForks.appendChild(beam);
      } else {
        fx = cx + forkRestRadius * Math.cos(baseAngle);
        fy = cy + forkRestRadius * Math.sin(baseAngle);
        rotAngle = (baseAngle * 180) / Math.PI + 90;
      }

      const forkGroup = document.createElementNS('http://www.w3.org/2000/svg', 'g');
      forkGroup.setAttribute('transform', `translate(${fx}, ${fy}) rotate(${rotAngle})`);

      const forkProngs = document.createElementNS('http://www.w3.org/2000/svg', 'path');
      forkProngs.setAttribute('d', 'M 0 -14 L 0 10 M -4 -14 L -4 -4 L 4 -4 L 4 -14 M -2 -14 L -2 -4 M 2 -14 L 2 -4');
      forkProngs.setAttribute('stroke', strokeColor);
      forkProngs.setAttribute('stroke-width', isHeld ? '2.5' : '1.8');
      forkProngs.setAttribute('stroke-linecap', 'round');
      forkProngs.setAttribute('fill', 'none');

      const forkBadge = document.createElementNS('http://www.w3.org/2000/svg', 'text');
      forkBadge.setAttribute('x', '0');
      forkBadge.setAttribute('y', '22');
      forkBadge.setAttribute('text-anchor', 'middle');
      forkBadge.setAttribute('fill', isHeld ? strokeColor : '#64748b');
      forkBadge.setAttribute('font-size', '9');
      forkBadge.setAttribute('font-family', 'JetBrains Mono');
      forkBadge.setAttribute('font-weight', '700');
      forkBadge.textContent = `F${fork.id}`;

      forkGroup.appendChild(forkProngs);
      forkGroup.appendChild(forkBadge);
      el.layerForks.appendChild(forkGroup);
    }

    // Philosophers (Crisp Luminous White Glass Spheres)
    for (let i = 0; i < n; i++) {
      const p = philosophers[i];
      const pos = philPositions[i];

      let stateColor = '#0284c7'; // Thinking (Sky Blue)
      let emoji = '🤔';
      let stateGlowClass = '';

      if (p.state === 'HUNGRY') {
        stateColor = '#d97706'; // Hungry (Sunset Gold)
        emoji = '🤤';
        stateGlowClass = 'hungry-pulse';
      } else if (p.state === 'EATING') {
        stateColor = '#059669'; // Eating (Emerald Mint)
        emoji = '🍝';
        if (previousEatingStates.get(p.id) !== 'EATING') {
          triggerEatingParticleBurst(pos.x, pos.y);
        }
      } else if (p.state === 'BLOCKED') {
        stateColor = '#ea580c'; // Blocked (Persimmon Orange)
        emoji = '🛑';
      }
      previousEatingStates.set(p.id, p.state);

      const pGroup = document.createElementNS('http://www.w3.org/2000/svg', 'g');

      if (p.state === 'EATING') {
        const glowAura = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
        glowAura.setAttribute('cx', pos.x);
        glowAura.setAttribute('cy', pos.y);
        glowAura.setAttribute('r', '39');
        glowAura.setAttribute('fill', 'none');
        glowAura.setAttribute('stroke', '#10b981');
        glowAura.setAttribute('stroke-width', '4');
        glowAura.setAttribute('filter', 'url(#glow-eating)');
        pGroup.appendChild(glowAura);

        const progressRing = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
        progressRing.setAttribute('cx', pos.x);
        progressRing.setAttribute('cy', pos.y);
        progressRing.setAttribute('r', '34');
        progressRing.setAttribute('class', 'eating-progress-ring');
        pGroup.appendChild(progressRing);
      }

      // Crisp White Glass Orb Body
      const avatarCircle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
      avatarCircle.setAttribute('cx', pos.x);
      avatarCircle.setAttribute('cy', pos.y);
      avatarCircle.setAttribute('r', '30');
      avatarCircle.setAttribute('fill', '#ffffff');
      avatarCircle.setAttribute('stroke', stateColor);
      avatarCircle.setAttribute('stroke-width', '3.5');
      if (stateGlowClass) avatarCircle.setAttribute('class', stateGlowClass);
      pGroup.appendChild(avatarCircle);

      // Emoji
      const iconText = document.createElementNS('http://www.w3.org/2000/svg', 'text');
      iconText.setAttribute('x', pos.x);
      iconText.setAttribute('y', pos.y - 4);
      iconText.setAttribute('text-anchor', 'middle');
      iconText.setAttribute('font-size', '16');
      iconText.textContent = emoji;
      pGroup.appendChild(iconText);

      // Name (High Contrast Dark Slate)
      const nameText = document.createElementNS('http://www.w3.org/2000/svg', 'text');
      nameText.setAttribute('x', pos.x);
      nameText.setAttribute('y', pos.y + 16);
      nameText.setAttribute('text-anchor', 'middle');
      nameText.setAttribute('fill', '#0f172a');
      nameText.setAttribute('font-size', '11');
      nameText.setAttribute('font-weight', '800');
      nameText.setAttribute('font-family', 'Inter');
      nameText.textContent = `P${p.id}`;
      pGroup.appendChild(nameText);

      // Meals count
      const mealsText = document.createElementNS('http://www.w3.org/2000/svg', 'text');
      mealsText.setAttribute('x', pos.x);
      mealsText.setAttribute('y', pos.y + 45);
      mealsText.setAttribute('text-anchor', 'middle');
      mealsText.setAttribute('fill', '#475569');
      mealsText.setAttribute('font-size', '10');
      mealsText.setAttribute('font-weight', '600');
      mealsText.setAttribute('font-family', 'JetBrains Mono');
      mealsText.textContent = `${p.mealsEaten} meals`;
      pGroup.appendChild(mealsText);

      el.layerPhilosophers.appendChild(pGroup);
    }
  }

  function triggerEatingParticleBurst(x, y) {
    const particleColors = ['#10b981', '#059669', '#34d399', '#0284c7', '#f59e0b'];
    for (let k = 0; k < 8; k++) {
      const p = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
      p.setAttribute('cx', x);
      p.setAttribute('cy', y);
      p.setAttribute('r', '3.5');
      p.setAttribute('fill', particleColors[k % particleColors.length]);
      el.layerPhilosophers.appendChild(p);

      const angle = (k / 8) * Math.PI * 2;
      const dist = 35 + Math.random() * 20;

      gsap.to(p, {
        attr: {
          cx: x + Math.cos(angle) * dist,
          cy: y + Math.sin(angle) * dist,
          r: 0
        },
        opacity: 0,
        duration: 0.75,
        ease: 'power2.out',
        onComplete: () => p.remove()
      });
    }
  }

  function initCharts() {
    const ctx = document.getElementById('chart-meals').getContext('2d');
    const gradient = ctx.createLinearGradient(0, 0, 0, 180);
    gradient.addColorStop(0, '#2563eb');
    gradient.addColorStop(1, '#059669');

    mealsChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: ['P0', 'P1', 'P2', 'P3', 'P4'],
        datasets: [{
          label: 'Meals',
          data: [0, 0, 0, 0, 0],
          backgroundColor: gradient,
          borderColor: '#1d4ed8',
          borderWidth: 1.5,
          borderRadius: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          x: {
            grid: { display: false },
            ticks: { color: '#475569', font: { family: 'JetBrains Mono', size: 10, weight: 'bold' } }
          },
          y: {
            beginAtZero: true,
            grid: { color: '#f1f5f9' },
            ticks: { precision: 0, color: '#475569', font: { family: 'JetBrains Mono', size: 10, weight: 'bold' } }
          }
        }
      }
    });
  }

  function updateMealsChart(state) {
    if (!mealsChart || !state.philosophers) return;
    const labels = state.philosophers.map(p => `P${p.id}`);
    const data = state.philosophers.map(p => p.mealsEaten);

    mealsChart.data.labels = labels;
    mealsChart.data.datasets[0].data = data;
    mealsChart.update('none');
  }

  function appendTerminalLog(type, message, timestamp) {
    if (!el.terminalBody) return;

    const timeStr = timestamp ? new Date(timestamp).toLocaleTimeString() : new Date().toLocaleTimeString();
    const line = document.createElement('div');

    let badgeColor = 'text-slate-400';
    if (type === 'EATING') badgeColor = 'text-emerald-400 font-bold';
    else if (type === 'BLOCKED') badgeColor = 'text-amber-400 font-semibold';
    else if (type === 'DEADLOCK_DETECTED') badgeColor = 'text-red-400 font-extrabold bg-red-950/60 px-1 rounded';
    else if (type === 'DEADLOCK_RECOVERED' || type === 'VICTIM_PREEMPTED') badgeColor = 'text-purple-400 font-bold';
    else if (type === 'FORK_ACQUIRED') badgeColor = 'text-sky-300';

    line.className = 'py-0.5 border-b border-white/[0.04] flex items-start gap-2 leading-relaxed';
    line.innerHTML = `
      <span class="text-slate-500 shrink-0 select-none">[${timeStr}]</span>
      <span class="${badgeColor} break-all">${escapeHtml(message)}</span>
    `;

    el.terminalBody.appendChild(line);
    el.terminalBody.scrollTop = el.terminalBody.scrollHeight;

    while (el.terminalBody.children.length > 250) {
      el.terminalBody.removeChild(el.terminalBody.firstChild);
    }
  }

  function renderHistoryTable(runs) {
    if (!el.historyTableBody) return;
    el.historyTableBody.innerHTML = '';

    if (!runs || runs.length === 0) {
      el.historyTableBody.innerHTML = `<tr><td colspan="9" class="p-6 text-center text-slate-400 italic">No runs recorded in database yet.</td></tr>`;
      return;
    }

    runs.forEach(run => {
      const tr = document.createElement('tr');
      tr.className = 'hover:bg-slate-50 transition';
      const timeFormatted = run.startTime ? new Date(run.startTime).toLocaleString() : '-';
      const deadlockBadge = run.deadlockOccurred
        ? `<span class="px-2 py-0.5 rounded text-[10px] font-extrabold bg-red-100 text-red-800 border border-red-200">DEADLOCK</span>`
        : `<span class="px-2 py-0.5 rounded text-[10px] font-extrabold bg-emerald-100 text-emerald-800 border border-emerald-200">SAFE</span>`;

      tr.innerHTML = `
        <td class="p-3 font-extrabold text-blue-700">#${run.id}</td>
        <td class="p-3 text-slate-900 font-sans font-semibold">${escapeHtml(run.strategy)}</td>
        <td class="p-3 font-semibold">${run.philosopherCount}</td>
        <td class="p-3 text-slate-500">${timeFormatted}</td>
        <td class="p-3 font-semibold">${run.durationSec ? run.durationSec.toFixed(1) : 0}s</td>
        <td class="p-3">${deadlockBadge}</td>
        <td class="p-3 font-bold text-slate-900">${run.totalMeals}</td>
        <td class="p-3 text-slate-600">${run.maxWaitMs}ms</td>
        <td class="p-3 text-slate-600">${run.fairnessMetric}</td>
      `;
      el.historyTableBody.appendChild(tr);
    });
  }

  function renderBenchmarkResults(results) {
    if (!el.benchmarkTableBody) return;
    el.benchmarkTableBody.innerHTML = '';

    if (!results || results.length === 0) {
      el.benchmarkTableBody.innerHTML = `<tr><td colspan="7" class="p-6 text-center text-slate-400 italic">No benchmark data.</td></tr>`;
      return;
    }

    let winnerStrategyKey = null;
    let maxMeals = -1;
    results.forEach(r => {
      if (!r.deadlockOccurred && r.totalMeals > maxMeals) {
        maxMeals = r.totalMeals;
        winnerStrategyKey = r.strategyKey;
      }
    });

    const labels = [];
    const mealsData = [];
    const waitData = [];

    results.forEach(res => {
      labels.push(res.strategyName);
      mealsData.push(res.totalMeals);
      waitData.push(res.maxWaitMs);

      const isWinner = res.strategyKey === winnerStrategyKey;
      const tr = document.createElement('tr');
      tr.className = isWinner
        ? 'bg-emerald-50 border-l-4 border-l-emerald-600 font-bold transition'
        : 'hover:bg-slate-50 transition';

      const dlBadge = res.deadlockOccurred
        ? `<span class="px-2 py-0.5 rounded text-[10px] font-bold bg-red-100 text-red-800 border border-red-200">Deadlocked</span>`
        : `<span class="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-200">Immune</span>`;

      const statusTag = isWinner
        ? `<span class="px-2.5 py-0.5 rounded text-[10px] font-extrabold bg-gradient-to-r from-emerald-600 to-teal-600 text-white shadow-sm flex items-center gap-1 w-max"><i data-lucide="crown" class="w-3 h-3"></i> TOP WINNER</span>`
        : `<span class="text-slate-400 text-[11px]">-</span>`;

      tr.innerHTML = `
        <td class="p-3 text-slate-900 font-bold">${escapeHtml(res.strategyName)}</td>
        <td class="p-3 text-blue-700 font-mono text-[11px] font-semibold">${escapeHtml(res.brokenCondition)}</td>
        <td class="p-3">${dlBadge}</td>
        <td class="p-3 font-mono text-emerald-700 font-bold text-sm">${res.totalMeals}</td>
        <td class="p-3 font-mono text-slate-600">${res.maxWaitMs}ms</td>
        <td class="p-3 font-mono text-slate-600">${res.fairnessMetric}</td>
        <td class="p-3">${statusTag}</td>
      `;
      el.benchmarkTableBody.appendChild(tr);
    });

    initLucide();
    renderBenchmarkCharts(labels, mealsData, waitData);
  }

  function renderBenchmarkCharts(labels, meals, wait) {
    const mealsCtx = document.getElementById('chart-benchmark-meals').getContext('2d');
    if (benchmarkMealsChart) benchmarkMealsChart.destroy();
    benchmarkMealsChart = new Chart(mealsCtx, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [{
          label: 'Total Meals',
          data: meals,
          backgroundColor: 'rgba(5, 150, 105, 0.8)',
          borderColor: '#059669',
          borderWidth: 1.5,
          borderRadius: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          x: { ticks: { color: '#475569', font: { size: 9, weight: 'bold' } } },
          y: { beginAtZero: true, ticks: { color: '#475569', font: { weight: 'bold' } } }
        }
      }
    });

    const waitCtx = document.getElementById('chart-benchmark-wait').getContext('2d');
    if (benchmarkWaitChart) benchmarkWaitChart.destroy();
    benchmarkWaitChart = new Chart(waitCtx, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [{
          label: 'Max Wait (ms)',
          data: wait,
          backgroundColor: 'rgba(220, 38, 38, 0.8)',
          borderColor: '#dc2626',
          borderWidth: 1.5,
          borderRadius: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          x: { ticks: { color: '#475569', font: { size: 9, weight: 'bold' } } },
          y: { beginAtZero: true, ticks: { color: '#475569', font: { weight: 'bold' } } }
        }
      }
    });
  }

  function startDemoMode() {
    demoActive = true;
    demoStep = 1;
    demoPaused = false;
    el.demoBanner.classList.remove('hidden');
    gsap.fromTo(el.demoBanner, { opacity: 0, y: 30 }, { opacity: 1, y: 0, duration: 0.4 });
    executeDemoStep();
  }

  function stopDemoMode() {
    demoActive = false;
    if (demoTimer) clearTimeout(demoTimer);
    el.demoBanner.classList.add('hidden');
    showToast('Demo Finished', 'Presentation mode exited.', 'info');
  }

  function toggleDemoPause() {
    demoPaused = !demoPaused;
    el.btnDemoPause.innerHTML = demoPaused
      ? `<i data-lucide="play" class="w-3.5 h-3.5"></i> Resume`
      : `<i data-lucide="pause" class="w-3.5 h-3.5"></i> Pause`;
    initLucide();
    if (!demoPaused) executeDemoStep();
  }

  function advanceDemoStep() {
    if (demoTimer) clearTimeout(demoTimer);
    demoStep++;
    if (demoStep > 3) {
      stopDemoMode();
    } else {
      executeDemoStep();
    }
  }

  function executeDemoStep() {
    if (!demoActive || demoPaused) return;

    if (demoStep === 1) {
      el.demoStepBadge.textContent = 'Step 1 of 3: Inducing Deadlock';
      el.demoCaptionTitle.textContent = 'Running Naive Strategy (Deadlock-Prone)';
      el.demoCaptionDesc.textContent = 'Philosophers pick left then right with 100ms pause. All 4 Coffman conditions hold, producing a circular wait.';
      el.demoProgressBar.style.width = '33%';

      el.selectStrategy.value = 'naive';
      el.selectStrategy.dispatchEvent(new Event('change'));

      startSimulation({ strategy: 'naive', philosophers: 5, thinkMs: 250, eatMs: 250 });

      demoTimer = setTimeout(() => {
        if (demoActive && !demoPaused) {
          demoStep = 2;
          executeDemoStep();
        }
      }, 4500);

    } else if (demoStep === 2) {
      el.demoStepBadge.textContent = 'Step 2 of 3: Preemption Recovery';
      el.demoCaptionTitle.textContent = 'Breaking No-Preemption via Victim Preemption';
      el.demoCaptionDesc.textContent = 'Deadlock detected! Simulator selects the victim thread with lowest meals and forcibly revokes its fork.';
      el.demoProgressBar.style.width = '66%';

      recoverDeadlock();

      demoTimer = setTimeout(() => {
        if (demoActive && !demoPaused) {
          demoStep = 3;
          executeDemoStep();
        }
      }, 4500);

    } else if (demoStep === 3) {
      el.demoStepBadge.textContent = 'Step 3 of 3: Deadlock Prevention';
      el.demoCaptionTitle.textContent = 'Switching to Max-Four Strategy (Semaphore Limit)';
      el.demoCaptionDesc.textContent = 'Semaphore limits diners to at most 4. By the Pigeonhole Principle, at least one philosopher always gets both forks!';
      el.demoProgressBar.style.width = '100%';

      el.selectStrategy.value = 'maxfour';
      el.selectStrategy.dispatchEvent(new Event('change'));

      startSimulation({ strategy: 'maxfour', philosophers: 5, thinkMs: 200, eatMs: 200 });

      demoTimer = setTimeout(() => {
        if (demoActive) {
          stopDemoMode();
          showToast('Demo Complete', 'Demonstrated Naive Deadlock -> Preemption Recovery -> Semaphore Prevention.', 'success');
        }
      }, 7000);
    }
  }

  function escapeHtml(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }
});
