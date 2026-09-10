import React from 'react';

interface NeoButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'paper' | 'outline' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  icon?: React.ReactNode;
}

export function NeoButton({
  children,
  variant = 'primary',
  size = 'md',
  icon,
  className = '',
  ...props
}: NeoButtonProps) {
  const variantStyles = {
    primary: 'bg-primary-container text-on-primary-container hover:bg-[#a5f01e]',
    secondary: 'bg-secondary-container text-white hover:opacity-90',
    paper: 'bg-paper-warm text-on-surface hover:bg-[#ebdcc7]',
    outline: 'bg-white text-on-surface hover:bg-gray-50',
    danger: 'bg-mood-anxiety-stress text-white hover:opacity-90',
  };

  const sizeStyles = {
    sm: 'px-3 py-1.5 text-xs font-bold gap-1',
    md: 'px-4 py-2 text-sm font-bold gap-2',
    lg: 'px-6 py-3 text-base font-bold gap-2.5',
  };

  return (
    <button
      className={`inline-flex items-center justify-center border-neo rounded-xl shadow-neo font-space uppercase tracking-tight transition-all duration-150 hover:-translate-x-0.5 hover:-translate-y-0.5 hover:shadow-neo-lg active:translate-x-1 active:translate-y-1 active:shadow-none cursor-pointer ${variantStyles[variant]} ${sizeStyles[size]} ${className}`}
      {...props}
    >
      {icon && <span className="shrink-0">{icon}</span>}
      <span>{children}</span>
    </button>
  );
}
