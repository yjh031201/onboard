import type { ButtonHTMLAttributes } from "react";

type ButtonVariant = "primary" | "secondary" | "danger" | "ghost";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  fullWidth?: boolean;
}

const VARIANT_CLASSES: Record<ButtonVariant, string> = {
  primary: "bg-[#6366f1] text-white font-bold hover:bg-[#5457e5]",
  secondary: "bg-[#f3f4f6] text-[#111827] font-medium hover:bg-[#e9ecf0]",
  danger: "bg-[#ef4444] text-white font-medium hover:bg-[#e13333]",
  ghost: "bg-transparent text-[#6366f1] font-medium hover:bg-[#eeeefe]",
};

export default function Button({
  variant = "primary",
  fullWidth = false,
  className = "",
  ...props
}: ButtonProps) {
  return (
    <button
      type="button"
      {...props}
      className={`flex items-center justify-center gap-1.5 rounded-lg px-4 py-2.5 text-[13px] transition-colors ${
        VARIANT_CLASSES[variant]
      } ${fullWidth ? "w-full" : ""} ${className}`}
    />
  );
}
