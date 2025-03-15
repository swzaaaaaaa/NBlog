<template>
  <div class="chat-wrapper">
    <div class="chat-container">
      <div class="chat-header">
        <h2>DeepSeek 对话</h2>
      </div>
      <div class="chat-messages" ref="chatMessages">
        <div v-for="(message, index) in chatMessages" :key="index" :class="['message', message.includes('你:') ? 'user-message' : 'bot-message']">
          <span>{{ message }}</span>
        </div>
      </div>
      <div class="input-container">
        <input v-model="userInput" type="text" placeholder="输入你的问题" @keydown.enter="sendMessage">
        <button @click="sendMessage">发送</button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      userInput: '',
      chatMessages: []
    };
  },
  methods: {
    async sendMessage() {
      if (this.userInput.trim() === '') return;

      // 显示用户消息
      this.chatMessages.push(`你: ${this.userInput}`);

      try {
        const response = await fetch('http://localhost:8090/deepseek', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ prompt: this.userInput })
        });

        const data = await response.json();
        const answer = data.answer;

        // 显示 DeepSeek 的回答
        this.chatMessages.push(`DeepSeek: ${answer}`);
      } catch (error) {
        console.error('请求出错:', error);
        this.chatMessages.push('请求出错，请稍后再试');
      }

      // 清空输入框
      this.userInput = '';
      this.$nextTick(() => {
        this.$refs.chatMessages.scrollTop = this.$refs.chatMessages.scrollHeight;
      });
    }
  }
};
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
</style>