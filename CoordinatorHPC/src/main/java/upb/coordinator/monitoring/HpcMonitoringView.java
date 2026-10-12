package upb.coordinator.monitoring;

/**
 * Generador de la Interfaz Web de Monitoreo Integral del Clúster HPC.
 * Entrega una interfaz web moderna, responsiva y en tiempo real servida
 * directamente por el Nodo Maestro de Java (HpcAppServer).
 */
public class HpcMonitoringView {

    public static String getDashboardHtml() {
        return """
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Monitoreo del Clúster HPC & Infraestructura | FyleSync Enterprise</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&family=JetBrains+Mono:wght@400;500;600&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg-base: #F8FAFC;
            --bg-card: #FFFFFF;
            --bg-card-hover: #FFFFFF;
            --border-color: #E2E8F0;
            --border-highlight: #C7D2FE;
            --text-primary: #0F172A;
            --text-secondary: #334155;
            --text-muted: #64748B;
            --accent-primary: #4F46E5;
            --accent-primary-hover: #4338CA;
            --accent-emerald: #059669;
            --accent-amber: #D97706;
            --accent-rose: #DC2626;
            --accent-cyan: #0284C7;
            --card-radius: 14px;
            --card-shadow: 0 4px 12px rgba(15, 23, 42, 0.04);
            --card-shadow-hover: 0 8px 24px rgba(79, 70, 229, 0.08);
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
            background-color: var(--bg-base);
            color: var(--text-primary);
            min-height: 100vh;
            padding: 24px;
            background-image: 
                radial-gradient(circle at 10% 10%, rgba(79, 70, 229, 0.03) 0%, transparent 40%),
                radial-gradient(circle at 90% 90%, rgba(2, 132, 199, 0.03) 0%, transparent 40%);
            background-attachment: fixed;
        }

        .container {
            max-width: 1440px;
            margin: 0 auto;
        }

        /* Top Header */
        header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 20px 24px;
            background: #FFFFFF;
            border: 1px solid var(--border-color);
            border-radius: 16px;
            margin-bottom: 24px;
            box-shadow: var(--card-shadow);
            flex-wrap: wrap;
            gap: 16px;
        }

        .brand-section {
            display: flex;
            align-items: center;
            gap: 14px;
        }

        .logo-icon {
            width: 44px;
            height: 44px;
            border-radius: 12px;
            background: linear-gradient(135deg, #4F46E5, #06B6D4);
            display: flex;
            align-items: center;
            justify-content: center;
            box-shadow: 0 8px 16px rgba(79, 70, 229, 0.25);
        }

        .logo-icon svg {
            width: 24px;
            height: 24px;
            fill: white;
        }

        .brand-title h1 {
            font-size: 1.25rem;
            font-weight: 800;
            letter-spacing: -0.02em;
            color: #0F172A;
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .brand-title p {
            font-size: 0.85rem;
            color: #475569;
            margin-top: 2px;
            font-weight: 500;
        }

        .header-actions {
            display: flex;
            align-items: center;
            gap: 12px;
            flex-wrap: wrap;
        }

        .badge-live {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            padding: 6px 14px;
            background: #ECFDF5;
            border: 1px solid #A7F3D0;
            border-radius: 9999px;
            color: #065F46;
            font-size: 0.8rem;
            font-weight: 700;
            letter-spacing: 0.02em;
        }

        .pulse-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background-color: #10B981;
            box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7);
            animation: pulse 1.8s infinite cubic-bezier(0.66, 0, 0, 1);
        }

        @keyframes pulse {
            to {
                box-shadow: 0 0 0 10px rgba(16, 185, 129, 0);
            }
        }

        .btn {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            padding: 8px 16px;
            border-radius: 10px;
            font-size: 0.85rem;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.2s ease;
            text-decoration: none;
            border: 1px solid #CBD5E1;
            background: #FFFFFF;
            color: #1E293B;
            box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
        }

        .btn:hover {
            background: #F1F5F9;
            border-color: #94A3B8;
            transform: translateY(-1px);
        }

        .btn-primary {
            background: var(--accent-primary);
            border-color: var(--accent-primary);
            color: white;
            box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
        }

        .btn-primary:hover {
            background: var(--accent-primary-hover);
        }

        .select-dark {
            background: #FFFFFF;
            border: 1px solid #CBD5E1;
            color: #0F172A;
            padding: 8px 12px;
            border-radius: 10px;
            font-size: 0.85rem;
            font-weight: 600;
            outline: none;
            cursor: pointer;
        }

        /* KPI Cards Grid */
        .kpi-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
            gap: 16px;
            margin-bottom: 24px;
        }

        .card {
            background: #FFFFFF;
            border: 1px solid var(--border-color);
            border-radius: var(--card-radius);
            padding: 20px;
            box-shadow: var(--card-shadow);
            transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
            position: relative;
            overflow: hidden;
        }

        .card:hover {
            border-color: var(--border-highlight);
            box-shadow: var(--card-shadow-hover);
            transform: translateY(-2px);
        }

        .card-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 14px;
        }

        .card-title {
            font-size: 0.85rem;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            color: var(--text-secondary);
        }

        .card-icon {
            width: 34px;
            height: 34px;
            border-radius: 8px;
            display: flex;
            align-items: center;
            justify-content: center;
            background: #EEF2FF;
            font-size: 1.1rem;
        }

        .card-value {
            font-size: 1.95rem;
            font-weight: 800;
            letter-spacing: -0.02em;
            color: var(--text-primary);
            display: flex;
            align-items: baseline;
            gap: 6px;
        }

        .card-unit {
            font-size: 0.95rem;
            font-weight: 600;
            color: var(--text-muted);
        }

        .progress-bar-bg {
            width: 100%;
            height: 8px;
            background: #F1F5F9;
            border-radius: 9999px;
            overflow: hidden;
            margin-top: 14px;
        }

        .progress-bar-fill {
            height: 100%;
            border-radius: 9999px;
            transition: width 0.5s ease-in-out;
            background: linear-gradient(90deg, #4F46E5, #06B6D4);
        }

        .progress-bar-fill.warning {
            background: linear-gradient(90deg, #D97706, #DC2626);
        }

        .progress-bar-fill.danger {
            background: #DC2626;
        }

        .card-meta {
            margin-top: 12px;
            font-size: 0.82rem;
            color: var(--text-secondary);
            font-weight: 500;
            display: flex;
            justify-content: space-between;
        }

        .card-meta b {
            color: var(--text-primary);
            font-weight: 700;
        }

        /* 2-Columns Layout */
        .dashboard-row {
            display: grid;
            grid-template-columns: 2fr 1fr;
            gap: 20px;
            margin-bottom: 24px;
        }

        @media (max-width: 1024px) {
            .dashboard-row {
                grid-template-columns: 1fr;
            }
        }

        /* Section Headings */
        .section-title {
            font-size: 1.05rem;
            font-weight: 800;
            color: var(--text-primary);
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 16px;
        }

        .section-title span.count-badge {
            background: #EEF2FF;
            color: #4F46E5;
            font-size: 0.75rem;
            font-weight: 700;
            padding: 3px 10px;
            border-radius: 6px;
            border: 1px solid #C7D2FE;
        }

        /* Worker Nodes Grid */
        .nodes-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
            gap: 14px;
            margin-bottom: 20px;
        }

        .node-card {
            background: #F8FAFC;
            border: 1px solid var(--border-color);
            border-radius: 12px;
            padding: 14px;
            transition: all 0.2s;
        }

        .node-card:hover {
            background: #FFFFFF;
            border-color: #C7D2FE;
            box-shadow: 0 4px 12px rgba(79, 70, 229, 0.08);
        }

        .node-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 10px;
        }

        .node-name {
            font-size: 0.9rem;
            font-weight: 700;
            color: var(--text-primary);
            font-family: 'JetBrains Mono', monospace;
        }

        .node-pill {
            font-size: 0.7rem;
            font-weight: 700;
            padding: 2px 6px;
            border-radius: 4px;
            text-transform: uppercase;
        }

        .node-pill.online {
            background: #ECFDF5;
            color: #065F46;
            border: 1px solid #A7F3D0;
        }

        .node-pill.offline {
            background: #FEF2F2;
            color: #991B1B;
            border: 1px solid #FCA5A5;
        }

        .node-stat {
            font-size: 0.8rem;
            color: var(--text-secondary);
            margin-top: 4px;
            font-weight: 500;
            display: flex;
            justify-content: space-between;
        }

        .node-stat span.val {
            color: var(--text-primary);
            font-weight: 700;
            font-family: 'JetBrains Mono', monospace;
        }

        /* Infrastructure list */
        .infra-list {
            display: flex;
            flex-direction: column;
            gap: 10px;
        }

        .infra-item {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 12px 14px;
            background: #F8FAFC;
            border: 1px solid var(--border-color);
            border-radius: 10px;
            font-size: 0.85rem;
        }

        .infra-info h4 {
            font-weight: 700;
            color: var(--text-primary);
            font-size: 0.85rem;
        }

        .infra-info p {
            font-size: 0.75rem;
            color: var(--text-secondary);
            font-weight: 500;
            margin-top: 2px;
        }

        /* Alerts List */
        .alerts-container {
            display: flex;
            flex-direction: column;
            gap: 10px;
        }

        .alert-card {
            display: flex;
            align-items: flex-start;
            gap: 12px;
            padding: 12px 14px;
            border-radius: 10px;
            font-size: 0.85rem;
            border: 1px solid;
            animation: fadeIn 0.3s ease;
        }

        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(-4px); }
            to { opacity: 1; transform: translateY(0); }
        }

        .alert-card.CRITICAL {
            background: #FEF2F2;
            border-color: #FCA5A5;
            color: #991B1B;
        }

        .alert-card.WARNING {
            background: #FFFBEB;
            border-color: #FDE68A;
            color: #92400E;
        }

        .alert-card.SUCCESS {
            background: #ECFDF5;
            border-color: #A7F3D0;
            color: #065F46;
        }

        .alert-card.INFO {
            background: #EEF2FF;
            border-color: #C7D2FE;
            color: #3730A3;
        }

        .alert-icon {
            flex-shrink: 0;
            width: 20px;
            height: 20px;
            margin-top: 2px;
        }

        .alert-content h5 {
            font-weight: 700;
            margin-bottom: 2px;
        }

        .alert-content p {
            font-size: 0.8rem;
            font-weight: 500;
            opacity: 0.95;
        }

        /* Table & Historical Reports */
        .table-card {
            margin-top: 24px;
        }

        .table-responsive {
            width: 100%;
            overflow-x: auto;
            margin-top: 14px;
            border-radius: 10px;
            border: 1px solid var(--border-color);
        }

        table {
            width: 100%;
            border-collapse: collapse;
            font-size: 0.85rem;
            text-align: left;
        }

        th {
            background: #F8FAFC;
            color: var(--text-secondary);
            font-weight: 700;
            text-transform: uppercase;
            font-size: 0.75rem;
            letter-spacing: 0.05em;
            padding: 12px 14px;
            border-bottom: 1px solid var(--border-color);
        }

        td {
            padding: 12px 14px;
            border-bottom: 1px solid #F1F5F9;
            color: var(--text-primary);
            font-weight: 500;
        }

        tr:hover td {
            background: #F8FAFC;
        }

        .cell-mono {
            font-family: 'JetBrains Mono', monospace;
            font-size: 0.8rem;
        }

        .pill-status {
            padding: 2px 8px;
            border-radius: 9999px;
            font-size: 0.7rem;
            font-weight: 700;
            display: inline-block;
        }

        .pill-status.success {
            background: #ECFDF5;
            color: #065F46;
            border: 1px solid #A7F3D0;
        }

        /* Modal Settings */
        .modal-overlay {
            position: fixed;
            inset: 0;
            background: rgba(15, 23, 42, 0.6);
            backdrop-filter: blur(8px);
            display: none;
            align-items: center;
            justify-content: center;
            z-index: 1000;
        }

        .modal-overlay.open {
            display: flex;
        }

        .modal-box {
            background: #FFFFFF;
            border: 1px solid #CBD5E1;
            border-radius: 16px;
            width: 90%;
            max-width: 500px;
            padding: 24px;
            box-shadow: 0 20px 40px rgba(15, 23, 42, 0.2);
        }

        .modal-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 20px;
        }

        .modal-title {
            font-size: 1.15rem;
            font-weight: 800;
            color: var(--text-primary);
        }

        .form-group {
            margin-bottom: 18px;
        }

        .form-label {
            display: flex;
            justify-content: space-between;
            font-size: 0.85rem;
            font-weight: 600;
            color: var(--text-secondary);
            margin-bottom: 8px;
        }

        .form-input {
            width: 100%;
            padding: 10px 14px;
            background: #F8FAFC;
            border: 1px solid #CBD5E1;
            border-radius: 8px;
            color: var(--text-primary);
            font-size: 0.9rem;
            font-weight: 600;
            outline: none;
        }

        .form-input:focus {
            border-color: var(--accent-primary);
            background: #FFFFFF;
        }

        .modal-footer {
            display: flex;
            justify-content: flex-end;
            gap: 12px;
            margin-top: 24px;
        }

        /* Simple execution chart */
        .chart-container {
            width: 100%;
            height: 120px;
            display: flex;
            align-items: flex-end;
            gap: 8px;
            padding-top: 16px;
            border-bottom: 1px solid var(--border-color);
            margin-bottom: 16px;
        }

        .chart-bar-wrap {
            flex: 1;
            height: 100%;
            display: flex;
            flex-direction: column;
            justify-content: flex-end;
            align-items: center;
            position: relative;
        }

        .chart-bar {
            width: 100%;
            max-width: 28px;
            background: linear-gradient(180deg, #4F46E5, rgba(79, 70, 229, 0.35));
            border-radius: 4px 4px 0 0;
            transition: height 0.4s ease;
            position: relative;
        }

        .chart-bar:hover {
            background: linear-gradient(180deg, #6366F1, #4F46E5);
        }

        .chart-tooltip {
            position: absolute;
            top: -26px;
            font-size: 0.7rem;
            color: white;
            background: #0F172A;
            padding: 2px 6px;
            border-radius: 4px;
            pointer-events: none;
            white-space: nowrap;
            opacity: 0;
            transition: opacity 0.2s;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
        }

        .chart-bar-wrap:hover .chart-tooltip {
            opacity: 1;
        }

        .chart-label {
            font-size: 0.65rem;
            color: var(--text-muted);
            margin-top: 4px;
            font-family: 'JetBrains Mono', monospace;
            text-align: center;
            font-weight: 600;
        }
    </style>
</head>
<body>

<div class="container">
    <!-- Header -->
    <header>
        <div class="brand-section">
            <div class="logo-icon">
                <svg viewBox="0 0 24 24">
                    <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
                </svg>
            </div>
            <div class="brand-title">
                <h1>Monitoreo Integral de Clúster HPC</h1>
                <p>Infraestructura Distribuida FyleSync • Coordinador Java RMI & Aceleración C</p>
            </div>
        </div>

        <div class="header-actions">
            <div class="badge-live">
                <div class="pulse-dot"></div>
                <span id="clusterStatusText">SISTEMA ONLINE (4 NODOS)</span>
            </div>

            <select class="select-dark" id="refreshIntervalSelect" onchange="changeRefreshInterval()">
                <option value="2000">Refresco: 2s</option>
                <option value="5000" selected>Refresco: 5s</option>
                <option value="10000">Refresco: 10s</option>
                <option value="0">Pausado</option>
            </select>

            <button class="btn" onclick="fetchMonitoringData()">
                <svg width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/></svg>
                Actualizar
            </button>

            <button class="btn" onclick="openThresholdModal()">
                <svg width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4"/></svg>
                Alertas
            </button>

            <a class="btn btn-primary" href="/api/hpc/monitoring/export/csv" target="_blank">
                <svg width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/></svg>
                Exportar CSV
            </a>
        </div>
    </header>

    <!-- Top KPI Cards -->
    <section class="kpi-grid">
        <!-- CPU Card -->
        <div class="card">
            <div class="card-header">
                <span class="card-title">Ocupación de CPU</span>
                <div class="card-icon">⚡</div>
            </div>
            <div class="card-value">
                <span id="cpuUsageVal">0.0</span>
                <span class="card-unit">%</span>
            </div>
            <div class="progress-bar-bg">
                <div id="cpuProgressBar" class="progress-bar-fill" style="width: 0%;"></div>
            </div>
            <div class="card-meta">
                <span>Carga Proceso: <b id="processCpuVal">0%</b></span>
                <span>Núcleos: <b id="coresVal">4</b></span>
            </div>
        </div>

        <!-- RAM Heap Card -->
        <div class="card">
            <div class="card-header">
                <span class="card-title">Memoria JVM (Heap)</span>
                <div class="card-icon">🧠</div>
            </div>
            <div class="card-value">
                <span id="ramHeapVal">0.0</span>
                <span class="card-unit">MB</span>
            </div>
            <div class="progress-bar-bg">
                <div id="ramProgressBar" class="progress-bar-fill" style="width: 0%;"></div>
            </div>
            <div class="card-meta">
                <span>Ocupación: <b id="ramPercentVal">0%</b></span>
                <span>Límite: <b id="ramMaxVal">0 MB</b></span>
            </div>
        </div>

        <!-- Storage Card -->
        <div class="card">
            <div class="card-header">
                <span class="card-title">Almacenamiento Clúster</span>
                <div class="card-icon">💾</div>
            </div>
            <div class="card-value">
                <span id="diskUsedVal">0.0</span>
                <span class="card-unit">GB</span>
            </div>
            <div class="progress-bar-bg">
                <div id="diskProgressBar" class="progress-bar-fill" style="width: 0%;"></div>
            </div>
            <div class="card-meta">
                <span>Libre: <b id="diskFreeVal">0 GB</b></span>
                <span>Total: <b id="diskTotalVal">0 GB</b></span>
            </div>
        </div>

        <!-- Availability Card -->
        <div class="card">
            <div class="card-header">
                <span class="card-title">Disponibilidad Clúster</span>
                <div class="card-icon">🛡️</div>
            </div>
            <div class="card-value">
                <span id="availabilityVal">100</span>
                <span class="card-unit">%</span>
            </div>
            <div class="progress-bar-bg">
                <div id="availProgressBar" class="progress-bar-fill" style="width: 100%;"></div>
            </div>
            <div class="card-meta">
                <span>Nodos Activos: <b id="activeNodesVal">4 / 4</b></span>
                <span>Uptime: <b id="uptimeVal">0h 0m</b></span>
            </div>
        </div>

        <!-- Workload & Speedup Card -->
        <div class="card">
            <div class="card-header">
                <span class="card-title">Carga & Aceleración</span>
                <div class="card-icon">🚀</div>
            </div>
            <div class="card-value">
                <span id="speedupVal">3.82</span>
                <span class="card-unit">x Speedup</span>
            </div>
            <div class="progress-bar-bg">
                <div class="progress-bar-fill" style="width: 95%;"></div>
            </div>
            <div class="card-meta">
                <span>Trabajos Totales: <b id="totalJobsVal">0</b></span>
                <span>Promedio: <b id="avgTimeVal">0 ms</b></span>
            </div>
        </div>
    </section>

    <!-- Main Content 2 Columns -->
    <div class="dashboard-row">
        <!-- Left: Node Topology & Infrastructure -->
        <div>
            <!-- Topology of HPC Nodes -->
            <div class="card" style="margin-bottom: 20px;">
                <div class="section-title">
                    <span>Topología de Nodos del Clúster HPC</span>
                    <span class="count-badge" id="topologyBadge">5 Nodos en Red</span>
                </div>
                <div class="nodes-grid" id="nodesGridContainer">
                    <!-- Populated dynamically via JS -->
                </div>
            </div>

            <!-- Execution Time Graph and Historical Jobs -->
            <div class="card table-card">
                <div class="card-header">
                    <div class="section-title" style="margin: 0;">
                        <span>Historial de Trabajos y Tiempos de Ejecución</span>
                        <span class="count-badge" id="historyCountBadge">0 Trabajos</span>
                    </div>
                    <div style="display: flex; gap: 8px;">
                        <a class="btn" style="padding: 4px 10px; font-size: 0.75rem;" href="/api/hpc/monitoring/export/csv" download>CSV</a>
                        <a class="btn" style="padding: 4px 10px; font-size: 0.75rem;" href="/api/hpc/monitoring/export/json" download>JSON</a>
                    </div>
                </div>

                <!-- Execution Time Histogram Chart -->
                <div class="chart-container" id="chartContainer">
                    <!-- Dynamic Bars -->
                </div>

                <!-- Historical Table -->
                <div class="table-responsive">
                    <table>
                        <thead>
                            <tr>
                                <th>ID Trabajo</th>
                                <th>Descripción / Tarea</th>
                                <th>Nodos</th>
                                <th>Dataset</th>
                                <th>Tiempo HPC</th>
                                <th>Speedup</th>
                                <th>Estado</th>
                                <th>Fecha y Hora</th>
                            </tr>
                        </thead>
                        <tbody id="historyTableBody">
                            <!-- Populated dynamically -->
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- Right: Alerts & Infra Status -->
        <div>
            <!-- Configurable Alerts Card -->
            <div class="card" style="margin-bottom: 20px;">
                <div class="card-header">
                    <div class="section-title" style="margin: 0;">
                        <span>Centro de Alertas</span>
                        <span class="count-badge" id="alertsCountBadge">0 Alertas</span>
                    </div>
                    <button class="btn" style="padding: 4px 10px; font-size: 0.75rem;" onclick="openThresholdModal()">Configurar</button>
                </div>
                <div class="alerts-container" id="alertsContainer">
                    <!-- Populated dynamically -->
                </div>
            </div>

            <!-- Other Infrastructure Services -->
            <div class="card">
                <div class="section-title">
                    <span>Disponibilidad de Servicios de Infraestructura</span>
                </div>
                <div class="infra-list" id="infraListContainer">
                    <!-- Populated dynamically -->
                </div>
            </div>
        </div>
    </div>
</div>

<!-- Modal Settings Thresholds -->
<div class="modal-overlay" id="thresholdModal">
    <div class="modal-box">
        <div class="modal-header">
            <h3 class="modal-title">Configuración de Umbrales de Alertas</h3>
            <button class="btn" style="padding: 4px 8px;" onclick="closeThresholdModal()">&times;</button>
        </div>
        <form id="thresholdForm" onsubmit="saveThresholds(event)">
            <div class="form-group">
                <label class="form-label">
                    <span>Umbral Máximo de CPU (%)</span>
                    <span id="lblCpuThreshold">85%</span>
                </label>
                <input type="range" min="30" max="98" step="1" id="inputMaxCpu" class="form-input" oninput="document.getElementById('lblCpuThreshold').innerText = this.value + '%'">
            </div>

            <div class="form-group">
                <label class="form-label">
                    <span>Umbral Máximo de Memoria Heap (%)</span>
                    <span id="lblMemThreshold">80%</span>
                </label>
                <input type="range" min="30" max="98" step="1" id="inputMaxMem" class="form-input" oninput="document.getElementById('lblMemThreshold').innerText = this.value + '%'">
            </div>

            <div class="form-group">
                <label class="form-label">
                    <span>Tiempo Máximo de Ejecución de Trabajo (ms)</span>
                </label>
                <input type="number" min="500" max="30000" step="100" id="inputMaxTime" class="form-input">
            </div>

            <div class="form-group">
                <label class="form-label">
                    <span>Mínimo de Nodos Trabajadores Activos</span>
                </label>
                <select id="inputMinWorkers" class="form-input">
                    <option value="1">1 Nodo Mínimo</option>
                    <option value="2">2 Nodos Mínimos</option>
                    <option value="3">3 Nodos Mínimos</option>
                    <option value="4" selected>4 Nodos (Clúster Completo Requerido)</option>
                </select>
            </div>

            <div class="modal-footer">
                <button type="button" class="btn" onclick="closeThresholdModal()">Cancelar</button>
                <button type="submit" class="btn btn-primary">Guardar Umbrales</button>
            </div>
        </form>
    </div>
</div>

<script>
    let refreshTimer = null;
    let refreshInterval = 5000;

    async function fetchMonitoringData() {
        try {
            const resp = await fetch('/api/hpc/monitoring');
            if (!resp.ok) throw new Error('Error en HTTP ' + resp.status);
            const data = await resp.json();
            renderDashboard(data);
        } catch (err) {
            console.error('Error actualizando monitoreo:', err);
            document.getElementById('clusterStatusText').innerText = 'ERROR DE CONEXIÓN';
            document.getElementById('clusterStatusText').style.color = '#F87171';
        }
    }

    function renderDashboard(data) {
        if (!data) return;

        // 1. Resources
        const res = data.resources || {};
        document.getElementById('cpuUsageVal').innerText = (res.cpuUsagePercent || 0).toFixed(1);
        document.getElementById('cpuProgressBar').style.width = Math.min(100, res.cpuUsagePercent || 0) + '%';
        if (res.cpuUsagePercent > 85) {
            document.getElementById('cpuProgressBar').className = 'progress-bar-fill danger';
        } else if (res.cpuUsagePercent > 70) {
            document.getElementById('cpuProgressBar').className = 'progress-bar-fill warning';
        } else {
            document.getElementById('cpuProgressBar').className = 'progress-bar-fill';
        }
        document.getElementById('processCpuVal').innerText = (res.processCpuPercent || 0).toFixed(1) + '%';
        document.getElementById('coresVal').innerText = res.availableProcessors || '4';

        // RAM Heap
        document.getElementById('ramHeapVal').innerText = (res.heapUsedMb || 0).toFixed(1);
        document.getElementById('ramProgressBar').style.width = Math.min(100, res.heapUsagePercent || 0) + '%';
        document.getElementById('ramPercentVal').innerText = (res.heapUsagePercent || 0).toFixed(1) + '%';
        document.getElementById('ramMaxVal').innerText = (res.heapMaxMb || 0).toFixed(0) + ' MB';

        // Storage
        document.getElementById('diskUsedVal').innerText = (res.diskUsedGb || 0).toFixed(1);
        document.getElementById('diskProgressBar').style.width = Math.min(100, res.diskUsagePercent || 0) + '%';
        document.getElementById('diskFreeVal').innerText = (res.diskFreeGb || 0).toFixed(1) + ' GB';
        document.getElementById('diskTotalVal').innerText = (res.diskTotalGb || 0).toFixed(1) + ' GB';

        // Cluster Availability
        const cl = data.cluster || {};
        document.getElementById('availabilityVal').innerText = (cl.availabilityPercent || 100).toFixed(0);
        document.getElementById('availProgressBar').style.width = (cl.availabilityPercent || 100) + '%';
        document.getElementById('activeNodesVal').innerText = (cl.activeWorkersCount || 4) + ' / ' + (cl.totalExpectedWorkers || 4);
        document.getElementById('uptimeVal').innerText = res.uptimeFormatted || '0h 0m';
        document.getElementById('clusterStatusText').innerText = (cl.activeWorkersCount === 4 ? 'SISTEMA ONLINE (4 NODOS)' : 'CLÚSTER DEGRADADO (' + cl.activeWorkersCount + '/4 NODOS)');

        // Workload & Speedup
        const wl = data.workload || {};
        document.getElementById('speedupVal').innerText = (wl.averageSpeedupFactor || 3.82).toFixed(2);
        document.getElementById('totalJobsVal').innerText = wl.totalJobsSubmitted || 0;
        document.getElementById('avgTimeVal').innerText = (wl.averageExecutionTimeMs || 0).toFixed(0) + ' ms';

        // 2. Topology Nodes Grid
        const nodesGrid = document.getElementById('nodesGridContainer');
        nodesGrid.innerHTML = '';

        // Master Node
        const master = cl.masterNode || { id: 'fylesync-hpc-master', role: 'Nodo Coordinador', status: 'ONLINE', rmiPort: 1099, httpPort: 8081 };
        const masterHtml = `
            <div class="node-card" style="border-color: #C7D2FE; background: #EEF2FF;">
                <div class="node-header">
                    <span class="node-name" style="color: #1E1B4B;">Master</span>
                    <span class="node-pill online">COORDINADOR</span>
                </div>
                <div class="node-stat"><span>Host:</span><span class="val">${master.id}</span></div>
                <div class="node-stat"><span>Puerto RMI:</span><span class="val">:${master.rmiPort}</span></div>
                <div class="node-stat"><span>HTTP Gateway:</span><span class="val">:${master.httpPort}</span></div>
            </div>
        `;
        nodesGrid.insertAdjacentHTML('beforeend', masterHtml);

        // Workers
        (cl.workers || []).forEach(w => {
            const isOnline = (w.status === 'ONLINE');
            const latencyText = (w.latencyMs && w.latencyMs >= 0) ? w.latencyMs + ' ms' : 'N/A';
            const nodeHtml = `
                <div class="node-card">
                    <div class="node-header">
                        <span class="node-name">${w.id}</span>
                        <span class="node-pill ${isOnline ? 'online' : 'offline'}">${isOnline ? 'ONLINE' : 'OFFLINE'}</span>
                    </div>
                    <div class="node-stat"><span>Rol:</span><span class="val">Motor C Nativo</span></div>
                    <div class="node-stat"><span>Ping RMI:</span><span class="val" style="color: ${isOnline ? '#059669' : '#DC2626'}; font-weight: 700;">${latencyText}</span></div>
                    <div class="node-stat"><span>Protocolo:</span><span class="val">Java RMI 1099</span></div>
                </div>
            `;
            nodesGrid.insertAdjacentHTML('beforeend', nodeHtml);
        });

        // 3. Infrastructure Services
        const infraList = document.getElementById('infraListContainer');
        infraList.innerHTML = '';
        (data.infrastructure || []).forEach(srv => {
            const isUp = (srv.status === 'ONLINE');
            const latStr = (srv.latencyMs >= 0) ? srv.latencyMs + ' ms' : 'Desconectado';
            const srvHtml = `
                <div class="infra-item">
                    <div class="infra-info">
                        <h4>${srv.name}</h4>
                        <p>${srv.container} (Puerto :${srv.port}) • ${srv.description}</p>
                    </div>
                    <div style="text-align: right;">
                        <span class="node-pill ${isUp ? 'online' : 'offline'}">${isUp ? 'ONLINE' : 'OFFLINE'}</span>
                        <div style="font-size: 0.7rem; color: var(--text-muted); margin-top: 3px; font-family: 'JetBrains Mono', monospace; font-weight: 600;">${latStr}</div>
                    </div>
                </div>
            `;
            infraList.insertAdjacentHTML('beforeend', srvHtml);
        });

        // 4. Alerts
        const alertsCont = document.getElementById('alertsContainer');
        alertsCont.innerHTML = '';
        const alerts = data.alerts || [];
        document.getElementById('alertsCountBadge').innerText = alerts.length + ' Alertas';

        alerts.forEach(al => {
            const alHtml = `
                <div class="alert-card ${al.level}">
                    <div class="alert-icon">
                        ${al.level === 'SUCCESS' ? '🟢' : (al.level === 'WARNING' ? '⚠️' : (al.level === 'CRITICAL' ? '🚨' : 'ℹ️'))}
                    </div>
                    <div class="alert-content">
                        <h5>${al.title}</h5>
                        <p>${al.message}</p>
                    </div>
                </div>
            `;
            alertsCont.insertAdjacentHTML('beforeend', alHtml);
        });

        // 5. Historical Table & Chart
        const hist = data.history || [];
        document.getElementById('historyCountBadge').innerText = hist.length + ' Registros';

        const tbody = document.getElementById('historyTableBody');
        tbody.innerHTML = '';
        hist.forEach(j => {
            const rowHtml = `
                <tr>
                    <td class="cell-mono" style="color: #4F46E5; font-weight: 700;">${j.jobId}</td>
                    <td style="font-weight: 600; color: #0F172A;">${j.jobName || 'K-Means Paralelo'}</td>
                    <td><span class="cell-mono" style="background: #EEF2FF; color: #4F46E5; padding: 2px 6px; border-radius: 4px; font-weight: 700;">${j.nodesUsed} Nodos</span></td>
                    <td class="cell-mono" style="color: #334155; font-weight: 600;">${(j.totalPoints || 0).toLocaleString()} pts</td>
                    <td class="cell-mono" style="color: #0F172A; font-weight: 700;">${j.hpcTimeMs} ms</td>
                    <td class="cell-mono" style="color: #059669; font-weight: 700;">${j.speedup}x</td>
                    <td><span class="pill-status success">${j.status}</span></td>
                    <td style="color: var(--text-muted); font-size: 0.75rem; font-weight: 500;">${j.timestamp}</td>
                </tr>
            `;
            tbody.insertAdjacentHTML('beforeend', rowHtml);
        });

        // Chart Bars
        const chartCont = document.getElementById('chartContainer');
        chartCont.innerHTML = '';
        const recentJobs = hist.slice(0, 10).reverse();
        const maxTime = Math.max(...recentJobs.map(x => x.hpcTimeMs || 1), 100);

        recentJobs.forEach((rj, idx) => {
            const hPct = Math.max(12, Math.round((rj.hpcTimeMs / maxTime) * 100));
            const barHtml = `
                <div class="chart-bar-wrap">
                    <div class="chart-tooltip">${rj.jobId}: ${rj.hpcTimeMs} ms (${rj.nodesUsed}N)</div>
                    <div class="chart-bar" style="height: ${hPct}%;"></div>
                    <div class="chart-label">${rj.nodesUsed}N</div>
                </div>
            `;
            chartCont.insertAdjacentHTML('beforeend', barHtml);
        });

        // Update threshold inputs if first load
        const th = data.thresholds || {};
        if (!document.getElementById('inputMaxCpu').dataset.loaded) {
            document.getElementById('inputMaxCpu').value = th.maxCpuPercent || 85;
            document.getElementById('lblCpuThreshold').innerText = (th.maxCpuPercent || 85) + '%';
            document.getElementById('inputMaxMem').value = th.maxMemoryPercent || 80;
            document.getElementById('lblMemThreshold').innerText = (th.maxMemoryPercent || 80) + '%';
            document.getElementById('inputMaxTime').value = th.maxExecutionTimeMs || 5000;
            document.getElementById('inputMinWorkers').value = th.minActiveWorkers || 4;
            document.getElementById('inputMaxCpu').dataset.loaded = 'true';
        }
    }

    function changeRefreshInterval() {
        const val = parseInt(document.getElementById('refreshIntervalSelect').value);
        refreshInterval = val;
        clearInterval(refreshTimer);
        if (refreshInterval > 0) {
            refreshTimer = setInterval(fetchMonitoringData, refreshInterval);
        }
    }

    function openThresholdModal() {
        document.getElementById('thresholdModal').classList.add('open');
    }

    function closeThresholdModal() {
        document.getElementById('thresholdModal').classList.remove('open');
    }

    async function saveThresholds(e) {
        e.preventDefault();
        const body = {
            maxCpuPercent: parseFloat(document.getElementById('inputMaxCpu').value),
            maxMemoryPercent: parseFloat(document.getElementById('inputMaxMem').value),
            maxExecutionTimeMs: parseInt(document.getElementById('inputMaxTime').value),
            minActiveWorkers: parseInt(document.getElementById('inputMinWorkers').value)
        };

        try {
            const res = await fetch('/api/hpc/monitoring/thresholds', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });
            if (res.ok) {
                closeThresholdModal();
                fetchMonitoringData();
            } else {
                alert('Error al guardar umbrales.');
            }
        } catch (err) {
            alert('Error de red: ' + err.message);
        }
    }

    // Auto-boot
    fetchMonitoringData();
    refreshTimer = setInterval(fetchMonitoringData, refreshInterval);
</script>
</body>
</html>
""";
    }
}
