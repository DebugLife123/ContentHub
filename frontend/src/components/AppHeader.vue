<template>
  <header class="topbar">
    <div class="topbar-inner">
      <RouterLink to="/" class="brand-mark">
        <span class="brand-dot"></span>
        <span>Content<span>Hub</span></span>
      </RouterLink>
      <nav class="main-nav">
        <RouterLink to="/">发现内容</RouterLink>
        <a href="#creators">创作者</a>
        <a href="#pricing">订阅方案</a>
      </nav>
      <div class="header-actions">
        <RouterLink to="/login" class="login-link">登录</RouterLink>
        <RouterLink to="/creator" class="creator-cta">开始创作 <span>↗</span></RouterLink>
      </div>
    </div>
  </header>
</template>