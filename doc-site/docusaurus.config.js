// @ts-check
const {themes: prismThemes} = require('prism-react-renderer');

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'Zipe Spring Boot Starters',
  tagline: '自製 Spring Boot Starter 集合，提供各種常用功能的自動配置模組',
  favicon: 'img/favicon.ico',
  url: 'https://zipe.github.io',
  baseUrl: '/',
  organizationName: 'zipe',
  projectName: 'zipe-spring-boot-starters',
  onBrokenLinks: 'warn',
  i18n: { defaultLocale: 'zh-Hant', locales: ['zh-Hant'] },
  markdown: { hooks: { onBrokenMarkdownLinks: 'warn' } },
  presets: [['classic', { docs: { sidebarPath: './sidebars.js', routeBasePath: '/' }, blog: false, theme: { customCss: './src/css/custom.css' } }]],
  themeConfig: {
    navbar: {
      title: 'Zipe Spring Boot Starters',
      items: [
        { type: 'docSidebar', sidebarId: 'docsSidebar', position: 'left', label: '文件' },
        { href: 'https://github.com/zipe', label: 'GitHub', position: 'right' },
      ],
    },
    footer: { style: 'dark', copyright: 'Copyright © 2026 Zipe. Built with Docusaurus.' },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
      additionalLanguages: ['java', 'markup', 'yaml', 'bash'],
    },
  },
};
module.exports = config;
