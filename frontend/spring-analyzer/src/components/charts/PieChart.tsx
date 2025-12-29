import { useRef, useEffect } from 'react';

interface Props {
  data: Record<string, number>;
  title: string;
  colors?: string[];
}

const defaultColors = [
  '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6',
  '#06b6d4', '#ec4899', '#84cc16', '#f97316', '#64748b'
];

export function PieChart({ data, title, colors = defaultColors }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const entries = Object.entries(data).filter(([, v]) => v > 0);
    const total = entries.reduce((sum, [, v]) => sum + v, 0);
    if (total === 0) return;

    const dpr = window.devicePixelRatio || 1;
    canvas.width = 300 * dpr;
    canvas.height = 200 * dpr;
    ctx.scale(dpr, dpr);

    const centerX = 100;
    const centerY = 100;
    const radius = 70;

    let startAngle = -Math.PI / 2;

    entries.forEach(([, value], i) => {
      const sliceAngle = (value / total) * Math.PI * 2;
      
      ctx.beginPath();
      ctx.moveTo(centerX, centerY);
      ctx.arc(centerX, centerY, radius, startAngle, startAngle + sliceAngle);
      ctx.closePath();
      ctx.fillStyle = colors[i % colors.length];
      ctx.fill();

      startAngle += sliceAngle;
    });

    ctx.fillStyle = '#fff';
    ctx.beginPath();
    ctx.arc(centerX, centerY, 35, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = '#1e293b';
    ctx.font = 'bold 16px system-ui';
    ctx.textAlign = 'center';
    ctx.fillText(total.toString(), centerX, centerY + 5);

    let legendY = 20;
    entries.forEach(([label, value], i) => {
      ctx.fillStyle = colors[i % colors.length];
      ctx.fillRect(210, legendY, 12, 12);
      
      ctx.fillStyle = '#64748b';
      ctx.font = '11px system-ui';
      ctx.textAlign = 'left';
      const pct = Math.round((value / total) * 100);
      ctx.fillText(`${label} (${pct}%)`, 228, legendY + 10);
      legendY += 20;
    });

  }, [data, colors]);

  return (
    <div className="chart-container">
      <h4>{title}</h4>
      <canvas ref={canvasRef} style={{ width: 300, height: 200 }} />
    </div>
  );
}
