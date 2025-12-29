import { useRef, useEffect } from 'react';

interface Props {
  data: Record<string, number>;
  title: string;
  color?: string;
}

export function BarChart({ data, title, color = '#3b82f6' }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const entries = Object.entries(data).filter(([, v]) => v > 0);
    if (entries.length === 0) return;

    const max = Math.max(...entries.map(([, v]) => v));

    const dpr = window.devicePixelRatio || 1;
    canvas.width = 300 * dpr;
    canvas.height = 180 * dpr;
    ctx.scale(dpr, dpr);

    const barWidth = Math.min(40, (280 / entries.length) - 10);
    const chartHeight = 130;
    const startX = 10;
    const startY = 150;

    entries.forEach(([label, value], i) => {
      const barHeight = (value / max) * chartHeight;
      const x = startX + i * (barWidth + 10);
      const y = startY - barHeight;

      ctx.fillStyle = color;
      ctx.fillRect(x, y, barWidth, barHeight);

      ctx.fillStyle = '#fff';
      ctx.font = 'bold 11px system-ui';
      ctx.textAlign = 'center';
      if (barHeight > 20) {
        ctx.fillText(value.toString(), x + barWidth / 2, y + 15);
      }

      ctx.fillStyle = '#64748b';
      ctx.font = '10px system-ui';
      ctx.save();
      ctx.translate(x + barWidth / 2, startY + 5);
      ctx.rotate(-0.5);
      ctx.textAlign = 'right';
      ctx.fillText(label.length > 10 ? label.slice(0, 8) + '..' : label, 0, 0);
      ctx.restore();
    });

  }, [data, color]);

  return (
    <div className="chart-container">
      <h4>{title}</h4>
      <canvas ref={canvasRef} style={{ width: 300, height: 180 }} />
    </div>
  );
}
