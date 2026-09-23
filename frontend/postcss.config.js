// Tailwind 与 Flowbite 已移除：
// 项目里从来没有写过 @tailwind 指令（也没有任何工具类），
// tailwind.config.js 里的 content 还把 node_modules/flowbite 整个扫了一遍，
// 属于纯粹的死配置。这里只保留真正需要的 autoprefixer。
module.exports = {
  plugins: {
    autoprefixer: {},
  },
}
