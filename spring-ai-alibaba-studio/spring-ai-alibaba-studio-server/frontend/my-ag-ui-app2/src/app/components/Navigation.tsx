'use client';

import React from 'react';

interface NavigationProps {
  activeTab: 'chat' | 'demo';
  onTabChange: (tab: 'chat' | 'demo') => void;
}

const Navigation: React.FC<NavigationProps> = ({ activeTab, onTabChange }) => {
  return (
    <div className="bg-white border-b border-gray-200">
      <div className="max-w-7xl mx-auto px-4">
        <div className="flex space-x-8">
          <button
            onClick={() => onTabChange('chat')}
            className={`py-4 px-1 border-b-2 font-medium text-sm ${
              activeTab === 'chat'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            💬 AG-UI Chat
          </button>
          <button
            onClick={() => onTabChange('demo')}
            className={`py-4 px-1 border-b-2 font-medium text-sm ${
              activeTab === 'demo'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            🎬 Event Demo
          </button>
        </div>
      </div>
    </div>
  );
};

export default Navigation; 