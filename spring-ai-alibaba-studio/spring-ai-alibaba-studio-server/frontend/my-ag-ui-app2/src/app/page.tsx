'use client';

import React, { useState } from 'react';
import Navigation from './components/Navigation';
import AguiChat from './components/AguiChat';
import AguiDemo from './components/AguiDemo';

export default function Home() {
  const [activeTab, setActiveTab] = useState<'chat' | 'demo'>('chat');

  return (
    <main className="h-screen flex flex-col">
      <Navigation activeTab={activeTab} onTabChange={setActiveTab} />
      <div className="flex-1">
        {activeTab === 'chat' ? <AguiChat /> : <AguiDemo />}
      </div>
    </main>
  );
}
