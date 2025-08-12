#!/bin/bash

# AG-UI Chat Demo 启动脚本

echo "🚀 启动 AG-UI Chat Demo..."
echo ""

# 检查Node.js是否安装
if ! command -v node &> /dev/null; then
    echo "❌ 错误: 未找到 Node.js，请先安装 Node.js 18+"
    exit 1
fi

# 检查npm是否安装
if ! command -v npm &> /dev/null; then
    echo "❌ 错误: 未找到 npm，请先安装 npm"
    exit 1
fi

# 显示版本信息
echo "📋 环境信息:"
echo "   Node.js: $(node --version)"
echo "   npm: $(npm --version)"
echo ""

# 检查依赖是否安装
if [ ! -d "node_modules" ]; then
    echo "📦 安装依赖..."
    npm install
    if [ $? -ne 0 ]; then
        echo "❌ 依赖安装失败"
        exit 1
    fi
    echo "✅ 依赖安装完成"
    echo ""
fi

# 检查后端配置
echo "🔧 检查配置..."
if [ -f ".env.local" ]; then
    echo "   ✅ 找到环境配置文件"
    echo "   📍 后端地址: $(grep NEXT_PUBLIC_API_BASE_URL .env.local | cut -d'=' -f2)"
else
    echo "   ⚠️  未找到环境配置文件，使用默认配置"
    echo "   📍 默认后端地址: http://localhost:8080"
fi
echo ""

# 启动应用
echo "🌐 启动前端应用..."
echo "   应用将在 http://localhost:3000 启动"
echo "   按 Ctrl+C 停止应用"
echo ""

npm run dev 