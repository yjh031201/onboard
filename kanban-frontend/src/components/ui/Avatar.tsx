interface AvatarProps {
  initial: string;
  size?: number;
  className?: string;
}

export default function Avatar({ initial, size = 34, className = "" }: AvatarProps) {
  return (
    <div
      style={{ width: size, height: size }}
      className={`flex shrink-0 items-center justify-center rounded-full bg-[#6366f1] ${className}`}
    >
      <span className="text-[12px] font-bold text-white" style={{ fontSize: size * 0.38 }}>
        {initial}
      </span>
    </div>
  );
}
