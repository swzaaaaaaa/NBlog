<template>
  <div class="chat-wrapper">
    <div class="chat-container">
      <div class="chat-header">
        <h2>DeepSeek 对话</h2>
      </div>
      <div class="chat-messages" ref="chatMessages">
        <!-- <div v-for="(message, index) in chatMessages" :key="index" :class="['message', message.isUser ? 'user-message' : 'bot-message']">
          <span>{{ message.content }}</span>
        </div> -->
          <div 
          v-for="message in chatMessages" 
          :key="message.id" 
          :class="['message', message.isUser ? 'user-message' : 'bot-message']"
        >
          <div v-if="!message.isLoading"  v-html="parseMarkdown(message.content)"></div>
          <div v-if="message.isLoading" class="loading-spinner"></div>
        </div>
      </div>
      <div class="input-container">
        <input v-model="userInput" type="text" placeholder="输入你的问题" @keydown.enter="sendMessage" :disabled="isSending">
        <button @click="sendMessage" :disabled="isSending" :class="{ 'disabled-btn': isSending }">发送</button>
      </div>
    </div>
  </div>
</template>

<script>
import MarkdownIt from 'markdown-it';

export default {
  data() {
    return {
      userInput: '',
      chatMessages: [],
      isSending: false,
      currentEventSource: null,
      isGet: true,
      md: new MarkdownIt({
        html: true,        // 允许 HTML 标签（需配合安全过滤）
        linkify: true,     // 自动将 URL 转换为链接
        typographer: true  // 转换特殊符号（如 -- 转 em dash）
      }),
      chatHistory: []
    };
  },
  mounted() {
    this.getChatHistory();
  },
  methods: {
    async getChatHistory() {
      try {
        const response = await fetch('http://localhost:8090/chat-history');
        const data = await response.json();
        this.chatMessages = data;
         // 使用 $nextTick 确保 DOM 更新后再执行回调函数
      this.$nextTick(() => {
          // 将聊天消息容器滚动到最底部，保证最新消息可见
          this.$refs.chatMessages.scrollTop = this.$refs.chatMessages.scrollHeight;
      });
        //this.chatMessages.push(data);
      } catch (error) {
        console.error('获取聊天记录失败:', error);
      }
    },
    sendMessage() {
      if (this.userInput.trim() === '') return;
      if (this.isSending) return;

      this.isSending = true;
      //this.chatMessages.push({ content: this.userInput, isUser: true });
      const userMessage = { 
        content: this.userInput, 
        isUser: true, 
        id: Date.now(),
        isLoading: false // 添加加载状态字段
      };
      this.chatMessages.push(userMessage);
      
      // 使用 $nextTick 确保 DOM 更新后再执行回调函数
      this.$nextTick(() => {
          // 将聊天消息容器滚动到最底部，保证最新消息可见
          this.$refs.chatMessages.scrollTop = this.$refs.chatMessages.scrollHeight;
      });

      const currentInput = this.userInput;
      this.userInput = '';

      // 关闭之前的连接
      if (this.currentEventSource) {
        this.currentEventSource.close();
      }

      this.currentEventSource = new EventSource(`http://localhost:8090/deepseek?prompt=${encodeURIComponent(currentInput)}`);

      this.isGet = true;
      this.chatMessages.push({  isLoading:true, isUser: false, id: Date.now() });

      this.currentEventSource.onmessage = (event) => {
         // 检查接收到的消息是否为结束标记 "[DONE]"
        if (event.data.trim() === "[DONE]") {
            console.log('连接已关闭');
            this.currentEventSource.close();
            this.isSending = false;
            return;
        }

        if (this.isGet == true && event.data.trim() != "[DONE]") {
          this.chatMessages.pop();
          this.isGet = false;
        }

        // 获取聊天消息列表中的最后一条消息
        const lastMessage = this.chatMessages[this.chatMessages.length - 1];
        // 判断最后一条消息是否为机器人消息
        if (!lastMessage.isUser) {
            // 如果是机器人消息，将新接收到的消息内容追加到最后一条消息的内容后面
            lastMessage.content += event.data;
        } else {
            // 如果最后一条消息是用户消息，创建一条新的机器人消息并添加到聊天消息列表中
            //this.chatMessages.push({ content: event.data, isUser: false });
            this.chatMessages.push({ content: event.data, isLoading:false, isUser: false, id: Date.now() });
     
        }

        // 使用 $nextTick 确保 DOM 更新后再执行回调函数
        this.$nextTick(() => {
            // 将聊天消息容器滚动到最底部，保证最新消息可见
            this.$refs.chatMessages.scrollTop = this.$refs.chatMessages.scrollHeight;
        });

       
      }

      this.currentEventSource.onerror = (error) => {
        console.error('连接错误:', error);
        this.isSending = false;
        this.currentEventSource.close();
      };
    },

    parseMarkdown(markdown) {
      // 安全增强：过滤危险标签（可选）
      const safeHtml = this.sanitizeHtml(this.md.render(markdown));
      return safeHtml;
    },

    sanitizeHtml(html) {
      // 简单 XSS 过滤（生产环境建议使用专业库如 DOMPurify）
      const temp = document.createElement('div');
      temp.innerHTML = html;
      
      // 允许的标签和属性
      const allowedTags = ['p', 'strong', 'em', 'a', 'ul', 'ol', 'li', 'br'];
      const allowedAttrs = ['href', 'title'];

      temp.querySelectorAll('*').forEach((el) => {
        if (!allowedTags.includes(el.tagName.toLowerCase())) {
          el.parentNode.removeChild(el);
          return;
        }

        Object.keys(el.attributes).forEach((attr) => {
          if (!allowedAttrs.includes(attr)) {
            el.removeAttribute(attr);
          }
        });
      });

      return temp.innerHTML;
    }
  }
}
</script>

<style scoped>
.chat-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f4f4f9;
}

.chat-container {
  width: 80%;
  height: 80vh;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  background-color: #fff;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.chat-header {
  background-color: #007bff;
  color: #fff;
  padding: 15px 20px;
  text-align: center;
  flex-shrink: 0;
}

.chat-header h2 {
  margin: 0;
  font-size: 1.3rem;
}

.chat-messages {
  flex-grow: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
}

.message {
  padding: 10px 15px;
  border-radius: 8px;
  margin-bottom: 10px;
  max-width: 80%;
  word-wrap: break-word;
}

.user-message {
  background-color: #e0f7fa;
  align-self: flex-end;
  color: #212121;
}

.bot-message {
  background-color: #f1f8e9;
  align-self: flex-start;
  color: #212121;
  
}

.bot-message ul,
.bot-message ol {
  padding-left: 20px; /* 缩进列表 */
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #e0e0e0;
  border-top-color: #007bff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-left: 10px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.input-container {
  display: flex;
  padding: 15px 20px;
  border-top: 1px solid #e0e0e0;
  flex-shrink: 0;
}

.input-container input {
  flex: 1;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 6px;
  margin-right: 10px;
  font-size: 1rem;
}

.input-container button {
  padding: 10px 20px;
  background-color: #007bff;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1rem;
  transition: background-color 0.2s ease;
}

.input-container button:hover {
  background-color: #0056b3;
}

.input-container button.disabled-btn {
  background-color: #ccc !important;
  cursor: not-allowed;
}
</style>