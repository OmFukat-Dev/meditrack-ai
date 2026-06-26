import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { LogOut, Activity, LayoutDashboard, Users, Calendar, MessageSquare, Settings } from 'lucide-react';
import { motion } from 'framer-motion';

export default function Sidebar({ onNavClick, activeNav }: { onNavClick?: (label: string) => void; activeNav?: string }) {
  const { user, logout } = useAuth();
  const [localActiveNav, setLocalActiveNav] = useState('Dashboard');
  const currentActive = activeNav !== undefined ? activeNav : localActiveNav;

  const navItems = [
    { icon: <LayoutDashboard size={20} />, label: 'Dashboard' },
    { icon: <Activity size={20} />, label: 'Reports' },
    { icon: <Users size={20} />, label: 'Patients' },
    { icon: <Calendar size={20} />, label: 'Calendar' },
    { icon: <MessageSquare size={20} />, label: 'Chat' },
    { icon: <Settings size={20} />, label: 'Settings' },
  ];

  return (
    <motion.div 
      initial={{ x: -250, opacity: 0 }}
      animate={{ x: 0, opacity: 1 }}
      transition={{ duration: 0.5, ease: [0.4, 0, 0.2, 1] }}
      className="w-[260px] h-[calc(100vh-32px)] my-4 ml-4 glass-panel flex flex-col p-6 z-20"
    >
      <div className="flex items-center gap-3 mb-10 px-2">
        <div className="w-10 h-10 rounded-xl bg-gradient-primary flex items-center justify-center shadow-glow">
          <Activity size={20} className="text-white" />
        </div>
        <span className="text-lg font-bold text-white tracking-tight">MediTrack AI</span>
      </div>

      <div className="flex flex-col gap-2 flex-1">
        {navItems.map((item, index) => {
          const isActive = currentActive === item.label;
          return (
            <div 
              key={index}
              onClick={() => {
                if (activeNav === undefined) {
                  setLocalActiveNav(item.label);
                }
                if (onNavClick) onNavClick(item.label);
              }}
              className={`flex items-center gap-3 px-4 py-3 rounded-xl cursor-pointer transition-all duration-300
                ${isActive 
                  ? 'bg-primary-500/10 text-primary-400 font-semibold' 
                  : 'text-dark-400 hover:text-white hover:bg-dark-800/50'
                }
              `}
            >
              {item.icon}
              <span>{item.label}</span>
            </div>
          );
        })}
      </div>

      <div className="mt-auto border-t border-white/10 pt-6">
        <div className="flex items-center gap-3 px-2 mb-6">
          <div className="w-10 h-10 rounded-full bg-dark-800 overflow-hidden border border-white/10">
            <img src={user?.avatar || '/avatar1.png'} alt="User" className="w-full h-full object-cover" />
          </div>
          <div className="flex flex-col">
            <span className="text-sm font-bold text-white">{user?.name}</span>
            <span className="text-xs text-dark-400 capitalize">{user?.role}</span>
          </div>
        </div>
        
        <button 
          onClick={logout}
          className="w-full flex items-center justify-center gap-2 py-3 rounded-xl bg-dark-800/50 text-dark-300 hover:bg-error-500/10 hover:text-error-400 transition-colors border border-transparent hover:border-error-500/20"
        >
          <LogOut size={18} />
          <span className="font-medium">Sign Out</span>
        </button>
      </div>
    </motion.div>
  );
}
