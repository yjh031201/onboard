import type { InputHTMLAttributes, TextareaHTMLAttributes } from "react";

interface FieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
}

export function Field({ label, id, ...inputProps }: FieldProps) {
  const inputId = id ?? label;
  return (
    <div className="flex w-full flex-col gap-2">
      <label htmlFor={inputId} className="text-[13px] font-medium text-[#111827]">
        {label}
      </label>
      <input
        id={inputId}
        {...inputProps}
        className="w-full rounded-lg border border-[#e5e7eb] px-3.5 py-3 text-[14px] text-[#111827] placeholder:text-[#9ca3af] focus:border-[#6366f1] focus:outline-none"
      />
    </div>
  );
}

interface TextareaFieldProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label: string;
}

export function TextareaField({ label, id, ...textareaProps }: TextareaFieldProps) {
  const inputId = id ?? label;
  return (
    <div className="flex w-full flex-col gap-2">
      <label htmlFor={inputId} className="text-[13px] font-medium text-[#111827]">
        {label}
      </label>
      <textarea
        id={inputId}
        {...textareaProps}
        className="w-full resize-none rounded-lg border border-[#e5e7eb] px-3.5 py-3 text-[14px] text-[#111827] placeholder:text-[#9ca3af] focus:border-[#6366f1] focus:outline-none"
      />
    </div>
  );
}
