import { useEffect, useRef } from "react";
import "./PrismaticBurst.css";

export default function PrismaticBurst({
  intensity = 2,
  speed = 0.5,
  animationType = "rotate3d",
  colors = ["#5227FF", "#FF9FFC", "#7cff67"],
  distort = 0,
  hoverDampness = 0,
  rayCount = 0,
}) {
  const canvasRef = useRef(null);
  const frameRef = useRef(null);
  const timeRef = useRef(0);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");

    const resize = () => {
      canvas.width = canvas.offsetWidth;
      canvas.height = canvas.offsetHeight;
    };
    resize();
    window.addEventListener("resize", resize);

    const totalRays = rayCount > 0 ? rayCount : 120;

    const draw = () => {
      timeRef.current += speed * 0.01;
      const t = timeRef.current;
      const w = canvas.width;
      const h = canvas.height;
      const cx = w / 2;
      const cy = h / 2;

      ctx.clearRect(0, 0, w, h);

      for (let i = 0; i < totalRays; i++) {
        const angle = (i / totalRays) * Math.PI * 2;
        const colorIndex = i % colors.length;
        const nextColorIndex = (i + 1) % colors.length;

        let rotatedAngle = angle;
        if (animationType === "rotate3d") {
          rotatedAngle = angle + t;
        } else if (animationType === "pulse") {
          rotatedAngle = angle + Math.sin(t) * 0.5;
        } else if (animationType === "wave") {
          rotatedAngle = angle + Math.sin(angle * 3 + t) * 0.3;
        }

        const distortion = distort > 0
          ? Math.sin(angle * 5 + t) * distort * 20
          : 0;

        const length = (Math.min(w, h) * 0.8 + distortion) * intensity;

        const x2 = cx + Math.cos(rotatedAngle) * length;
        const y2 = cy + Math.sin(rotatedAngle) * length;

        const grad = ctx.createLinearGradient(cx, cy, x2, y2);
        grad.addColorStop(0, colors[colorIndex] + "cc");
        grad.addColorStop(0.5, colors[nextColorIndex] + "66");
        grad.addColorStop(1, colors[colorIndex] + "00");

        ctx.beginPath();
        ctx.moveTo(cx, cy);
        ctx.lineTo(x2, y2);
        ctx.strokeStyle = grad;
        ctx.lineWidth = (w / totalRays) * 1.2;
        ctx.stroke();
      }

      frameRef.current = requestAnimationFrame(draw);
    };

    draw();

    return () => {
      window.removeEventListener("resize", resize);
      cancelAnimationFrame(frameRef.current);
    };
  }, [intensity, speed, animationType, colors, distort, rayCount]);

  return (
    <canvas
      ref={canvasRef}
      className="prismatic-burst"
      aria-hidden="true"
    />
  );
}