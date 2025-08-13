import React from 'react';
import { Card } from 'antd';
import { useConfigContext } from '../contexts/ConfigContext';
import Sidebar from './Sidebar';
import AguiChat from './AguiChat';
import DebugPanel from './DebugPanel';
import styles from '../index.module.less';

const ChatInterface: React.FC = () => {
  const { config } = useConfigContext();

  return (
    <div className={styles.chatInterface}>
      <Sidebar />
      <div className={styles.mainContent}>
        <div style={{ 
          padding: '24px', 
          height: '100%', 
          overflow: 'auto',
          marginTop: '16px' // 添加顶部边距，避免被导航栏遮挡
        }}>
          <Card 
            style={{ 
              height: '100%',
              boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
            }}
            bodyStyle={{ 
              padding: '24px',
              height: '100%'
            }}
          >
            <AguiChat />
          </Card>
        </div>
      </div>
      {config.showDebugInfo && <DebugPanel />}
    </div>
  );
};

export default ChatInterface;
