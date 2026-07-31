// Widget Übersicht — utilizzo del piano Claude.
// Installazione: copiare questo file in
//   ~/Library/Application Support/Übersicht/widgets/
// Richiede ~/.local/bin/claude-usage.sh (vedi install.sh).

export const command = "$HOME/.local/bin/claude-usage.sh";

// L'endpoint /api/oauth/usage ha un rate limit severo: lo script mantiene
// una cache, quindi qui si può aggiornare spesso senza fare richieste vere.
export const refreshFrequency = 60000;

export const className = `
  top: 40px;
  right: 40px;
  width: 260px;
  padding: 16px 18px;
  box-sizing: border-box;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", sans-serif;
  color: #f5f5f7;
  background: rgba(28, 28, 30, 0.72);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.10);
  border-radius: 16px;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.35);

  .cu-head {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    margin-bottom: 14px;
  }
  .cu-title {
    font-size: 12px;
    font-weight: 600;
    letter-spacing: 0.4px;
    text-transform: uppercase;
    opacity: 0.85;
  }
  .cu-flag { font-size: 10px; opacity: 0.55; }

  .cu-row { margin-bottom: 12px; }
  .cu-row:last-child { margin-bottom: 0; }

  .cu-label {
    display: flex;
    justify-content: space-between;
    font-size: 11px;
    margin-bottom: 5px;
  }
  .cu-name { opacity: 0.75; }
  .cu-val { font-variant-numeric: tabular-nums; font-weight: 600; }

  .cu-track {
    height: 6px;
    border-radius: 3px;
    background: rgba(255, 255, 255, 0.12);
    overflow: hidden;
  }
  .cu-fill {
    height: 100%;
    border-radius: 3px;
    transition: width 0.4s ease;
  }

  .cu-reset {
    margin-top: 4px;
    font-size: 10px;
    opacity: 0.5;
    font-variant-numeric: tabular-nums;
  }

  .cu-sub {
    display: flex;
    gap: 14px;
    margin-top: 12px;
    padding-top: 10px;
    border-top: 1px solid rgba(255, 255, 255, 0.10);
    font-size: 10px;
    opacity: 0.7;
    font-variant-numeric: tabular-nums;
  }

  .cu-msg { font-size: 11px; line-height: 1.5; opacity: 0.8; }
`;

const parse = (output) => {
  const data = {};
  (output || "").split("\n").forEach((line) => {
    const i = line.indexOf("=");
    if (i > 0) data[line.slice(0, i)] = line.slice(i + 1);
  });
  return data;
};

const colorFor = (pct) => {
  if (pct >= 90) return "#ff453a";
  if (pct >= 75) return "#ff9f0a";
  if (pct >= 50) return "#ffd60a";
  return "#32d74b";
};

const Bar = ({ name, pct, left }) => {
  if (pct === "" || pct === undefined) return null;
  const n = Math.min(100, Number(pct));
  return (
    <div className="cu-row">
      <div className="cu-label">
        <span className="cu-name">{name}</span>
        <span className="cu-val" style={{ color: colorFor(n) }}>
          {n}%
        </span>
      </div>
      <div className="cu-track">
        <div
          className="cu-fill"
          style={{ width: `${n}%`, background: colorFor(n) }}
        />
      </div>
      {left ? <div className="cu-reset">reset tra {left}</div> : null}
    </div>
  );
};

export const render = ({ output }) => {
  const d = parse(output);

  if (d.status !== "ok") {
    return (
      <div>
        <div className="cu-head">
          <span className="cu-title">Claude · utilizzo</span>
        </div>
        <div className="cu-msg">{d.error || "Nessun dato."}</div>
      </div>
    );
  }

  const subs = [];
  if (d.opus_pct) subs.push(`Opus ${d.opus_pct}%`);
  if (d.sonnet_pct) subs.push(`Sonnet ${d.sonnet_pct}%`);
  if (d.extra_pct) subs.push(`Extra ${d.extra_pct}%`);

  return (
    <div>
      <div className="cu-head">
        <span className="cu-title">Claude · utilizzo</span>
        {d.stale === "1" ? <span className="cu-flag">dato in cache</span> : null}
      </div>

      <Bar name="Finestra 5 ore" pct={d.five_hour_pct} left={d.five_hour_left} />
      <Bar name="Settimana" pct={d.seven_day_pct} left={d.seven_day_left} />

      {subs.length > 0 ? (
        <div className="cu-sub">
          {subs.map((s) => (
            <span key={s}>{s}</span>
          ))}
        </div>
      ) : null}
    </div>
  );
};
