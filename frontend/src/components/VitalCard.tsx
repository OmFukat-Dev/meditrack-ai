import React from 'react';
import { motion } from 'framer-motion';

interface VitalCardProps {
  icon: React.ReactNode;
  label: string;
  value: string;
  color?: string;
  trend?: string;
}

export default function VitalCard({ icon, label, value, color, trend }: VitalCardProps) {
  // Try to use a passed color or default to primary
  const textColorClass = color || 'text-primary-400';
  
  // Extract base color name (e.g. 'primary' from 'text-primary-400') to use for background tint
  const match = textColorClass.match(/text-([a-z]+)-/);
  const baseColor = match ? match[1] : 'primary';
  const bgClass = `bg-${baseColor}-500/10`;

  return (
    <motion.div 
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="glass-card p-6 flex flex-col h-full"
    >
      <div className="flex justify-between items-start mb-4">
        <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${bgClass} ${textColorClass}`}>
          {icon}
        </div>
        {trend && (
          <span className={`text-xs font-semibold px-2 py-1 rounded bg-dark-800/80 ${trend.startsWith('+') ? 'text-error-400' : 'text-success-400'}`}>
            {trend}
          </span>
        )}
      </div>
      <div className="mt-auto">
        <div className="text-dark-400 text-sm font-medium mb-1">{label}</div>
        <div className="text-2xl font-bold text-white tracking-tight">{value}</div>
      </div>
    </motion.div>
  );
}
