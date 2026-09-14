interface ToggleProps {
  checked: boolean;
  onChange: (next: boolean) => void;
  label?: string;
}

export default function Toggle({ checked, onChange, label }: ToggleProps) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={label}
      onClick={() => onChange(!checked)}
      className={`flex h-[22px] w-[38px] shrink-0 items-center rounded-full p-[3px] transition-colors ${
        checked ? "justify-end bg-[#6366f1]" : "justify-start bg-[#f3f4f6]"
      }`}
    >
      <span className="size-4 rounded-full bg-white shadow" />
    </button>
  );
}
