import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { LayoutDashboard, FileText, Calendar, MessageSquare, Settings, Bell, Activity, Users, LogOut } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import QuickCallMenu from './QuickCallMenu';

export default function TopNavbar({ onNavClick, activeNav }: { onNavClick?: (label: string) => void; activeNav?: string }) {
  const { user, logout } = useAuth();
  const [localActiveNav, setLocalActiveNav] = useState('Dashboard');
  const currentActive = activeNav !== undefined ? activeNav : localActiveNav;
  const [showNotifications, setShowNotifications] = useState(false);

  const notifications = [
    { id: 1, title: 'New Alert', message: 'SpO2 critical in Bed 102B', time: '2m ago', read: false },
    { id: 2, title: 'System Update', message: 'API Gateway successfully deployed', time: '1h ago', read: true },
    { id: 3, title: 'Message', message: 'Dr. Smith requested a consult', time: '3h ago', read: true }
  ];

  const navItems = [
    { icon: <LayoutDashboard size={18} />, label: 'Dashboard' },
    { icon: <Users size={18} />, label: 'Patients' },
    { icon: <FileText size={18} />, label: 'Reports' },
    { icon: <Calendar size={18} />, label: 'Calendar' },
    { icon: <MessageSquare size={18} />, label: 'Chat' },
    { icon: <Settings size={18} />, label: 'Settings' },
  ];

  return (
    <div className="w-full px-8 py-6 flex items-center justify-between z-20">
      
      {/* Left: Logo (Hidden on mobile) */}
      <div className="hidden md:flex items-center gap-3">
        <div className="text-primary-500">
          <Activity size={28} />
        </div>
        <span className="text-xl font-bold text-white tracking-tight">MediTrack</span>
      </div>

      {/* Center: Navigation Pill */}
      <div className="flex items-center bg-dark-900/60 backdrop-blur-md border border-white/5 rounded-full p-1.5 shadow-card">
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
              className={`flex items-center gap-2 px-4 py-2 rounded-full cursor-pointer transition-all duration-300
                ${isActive 
                  ? 'bg-dark-800 text-primary-400 shadow-inner' 
                  : 'text-dark-400 hover:text-white hover:bg-dark-800/50'
                }
              `}
            >
              {item.icon}
              <span className={`text-sm ${isActive ? 'font-semibold' : 'font-medium hidden lg:block'}`}>{item.label}</span>
            </div>
          );
        })}
      </div>

      {/* Right: Actions */}
      <div className="flex items-center gap-3 lg:gap-4">
        <QuickCallMenu />
        <div className="relative">
          <div 
            onClick={() => setShowNotifications(!showNotifications)}
            className="w-10 h-10 rounded-full bg-dark-800/50 border border-white/5 flex items-center justify-center cursor-pointer text-dark-300 hover:text-white hover:bg-dark-700 transition-colors relative"
          >
            <Bell size={18} />
            <span className="absolute top-2 right-2 w-2 h-2 bg-error-500 rounded-full shadow-[0_0_8px_rgba(239,68,68,0.8)] animate-pulse"></span>
          </div>
          
          <AnimatePresence>
            {showNotifications && (
              <motion.div 
                initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 10 }}
                className="absolute right-0 mt-3 w-80 bg-dark-800 border border-white/10 rounded-2xl shadow-card z-50 overflow-hidden"
              >
                <div className="p-4 border-b border-white/5 flex justify-between items-center bg-dark-900/50">
                  <h3 className="text-white font-bold text-sm">Notifications</h3>
                  <span className="text-primary-400 text-xs cursor-pointer hover:text-primary-300">Mark all read</span>
                </div>
                <div className="flex flex-col max-h-[300px] overflow-y-auto">
                  {notifications.map(n => (
                    <div key={n.id} className={`p-4 border-b border-white/5 hover:bg-dark-700/50 transition-colors cursor-pointer flex flex-col gap-1 ${!n.read ? 'bg-dark-800/80' : ''}`}>
                      <div className="flex justify-between items-start">
                        <span className={`text-sm font-semibold ${!n.read ? 'text-white' : 'text-dark-200'}`}>{n.title}</span>
                        <span className="text-[10px] text-dark-400">{n.time}</span>
                      </div>
                      <p className="text-xs text-dark-300">{n.message}</p>
                    </div>
                  ))}
                </div>
                <div className="p-3 text-center border-t border-white/5 bg-dark-900/50 cursor-pointer hover:bg-dark-700 transition-colors">
                  <span className="text-primary-400 text-xs font-semibold">View All Activity</span>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
        
        <div className="w-10 h-10 rounded-full bg-dark-800 overflow-hidden ml-2 border border-primary-500/30">
          <img src={user?.avatar || '/avatar1.png'} alt="User" className="w-full h-full object-cover" />
        </div>
        
        <button 
          onClick={logout}
          className="w-10 h-10 rounded-full bg-error-500/10 border border-error-500/20 flex items-center justify-center cursor-pointer text-error-400 hover:bg-error-500 hover:text-white transition-colors ml-2"
          title="Sign Out"
        >
          <LogOut size={18} />
        </button>
      </div>
    </div>
  );
}
