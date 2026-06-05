// @ts-check

/** @type {import('@docusaurus/plugin-content-docs').SidebarsConfig} */
const sidebars = {
  docsSidebar: [
    { type: 'doc', id: 'intro', label: '專案總覽' },
    {
      type: 'category',
      label: 'base-spring-boot-starter',
      link: { type: 'doc', id: 'base-starter/index' },
      items: [
        'base-starter/quickstart',
        'base-starter/configuration',
        'base-starter/examples',
        'base-starter/architecture',
      ],
    },
    {
      type: 'category',
      label: 'db-spring-boot-starter',
      link: { type: 'doc', id: 'db-starter/index' },
      items: [
        'db-starter/quickstart',
        'db-starter/configuration',
        'db-starter/examples',
        'db-starter/architecture',
      ],
    },
    {
      type: 'category',
      label: 'job-spring-boot-starter',
      link: { type: 'doc', id: 'job-starter/index' },
      items: [
        'job-starter/quickstart',
        'job-starter/configuration',
        'job-starter/examples',
        'job-starter/architecture',
      ],
    },
    {
      type: 'category',
      label: 'logon-spring-boot-starter',
      link: { type: 'doc', id: 'logon-starter/index' },
      items: [
        'logon-starter/quickstart',
        'logon-starter/configuration',
        'logon-starter/examples',
        'logon-starter/architecture',
      ],
    },
    {
      type: 'category',
      label: 'web-service-spring-boot-starter',
      link: { type: 'doc', id: 'web-service-starter/index' },
      items: [
        'web-service-starter/quickstart',
        'web-service-starter/configuration',
        'web-service-starter/examples',
        'web-service-starter/architecture',
      ],
    },
    {
      type: 'category',
      label: 'web-spring-boot-starter',
      link: { type: 'doc', id: 'web-starter/index' },
      items: [
        'web-starter/quickstart',
        'web-starter/configuration',
        'web-starter/examples',
        'web-starter/architecture',
      ],
    },
    {
      type: 'category',
      label: '整合範例教學',
      link: { type: 'doc', id: 'integration/index' },
      items: [
        'integration/scenario-web-auth',
        'integration/scenario-db',
        'integration/scenario-job',
        'integration/scenario-webservice',
        'integration/scenario-full',
      ],
    },
  ],
};

module.exports = sidebars;
