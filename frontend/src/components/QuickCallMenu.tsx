import { useState, type MouseEvent } from 'react';
import { IconButton, Menu, MenuItem, Tooltip } from '@mui/material';
import { Phone, ClipboardCopy } from 'lucide-react';

type ContactItem = {
  label: string;
  number: string;
  helper: string;
};

const contacts: ContactItem[] = [
  { label: 'Hospital operator', number: '+1-800-555-0100', helper: 'General front desk' },
  { label: 'On-call doctor', number: '+1-800-555-0101', helper: 'Clinical escalation' },
  { label: 'Nursing station', number: '+1-800-555-0102', helper: 'Ward coordination' },
  { label: 'Admin office', number: '+1-800-555-0103', helper: 'Registry and access' },
];

export default function QuickCallMenu() {
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);

  const handleOpen = (event: MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleCall = (number: string) => {
    window.location.href = `tel:${number}`;
    handleClose();
  };

  const handleCopy = async (number: string) => {
    try {
      await navigator.clipboard.writeText(number);
    } catch (error) {
      console.warn('Clipboard copy failed', error);
    }
  };

  return (
    <>
      <Tooltip title="Quick call">
        <IconButton
          onClick={handleOpen}
          sx={{
            width: 40,
            height: 40,
            bgcolor: 'rgba(15, 23, 42, 0.6)',
            border: '1px solid rgba(255, 255, 255, 0.06)',
            color: 'rgba(226, 232, 240, 0.9)',
            '&:hover': {
              bgcolor: 'rgba(30, 41, 59, 0.85)',
              color: '#fff',
            },
          }}
        >
          <Phone size={18} />
        </IconButton>
      </Tooltip>

      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleClose}
        PaperProps={{
          sx: {
            mt: 1.5,
            minWidth: 280,
            backgroundColor: 'rgba(15, 23, 42, 0.98)',
            border: '1px solid rgba(255, 255, 255, 0.08)',
            color: '#fff',
          },
        }}
      >
        {contacts.map((contact) => (
          <MenuItem
            key={contact.label}
            onClick={() => handleCall(contact.number)}
            sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 0.5, py: 1.25 }}
          >
            <span className="font-semibold">{contact.label}</span>
            <span className="text-xs text-slate-400">{contact.helper}</span>
            <span className="text-sm text-primary-300 mt-1">{contact.number}</span>
          </MenuItem>
        ))}

        <MenuItem
          onClick={() => {
            handleCopy(contacts[0].number);
            handleClose();
          }}
          sx={{ gap: 1 }}
        >
          <ClipboardCopy size={16} />
          Copy hospital operator number
        </MenuItem>
      </Menu>
    </>
  );
}
