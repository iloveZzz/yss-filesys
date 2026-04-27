import type { Component } from "vue";
import {
  DashboardOutlined,
  FileTextOutlined,
  DeleteOutlined,
  HeartOutlined,
  ShareAltOutlined,
  DatabaseOutlined,
  SwapOutlined,
} from "@ant-design/icons-vue";
import type { RouteRecordRaw } from "vue-router";

export const workspaceNav: Array<{
  title: string;
  path: string;
  icon: Component;
}> = [
  { title: "工作台", path: "/dashboard", icon: DashboardOutlined },
  { title: "文件", path: "/files", icon: FileTextOutlined },
  { title: "收藏", path: "/favorites", icon: HeartOutlined },
  { title: "回收站", path: "/recycle", icon: DeleteOutlined },
  { title: "分享", path: "/share", icon: ShareAltOutlined },
  { title: "存储", path: "/storage", icon: DatabaseOutlined },
  { title: "传输", path: "/transfer", icon: SwapOutlined },
];

export const routes: RouteRecordRaw[] = [
  {
    path: "/",
    redirect: "/dashboard",
  },
  {
    path: "/dashboard",
    name: "dashboard",
    component: () => import("@/views/Dashboard/index.vue"),
    meta: {
      title: "工作台",
      keepAlive: false,
    },
  },
  {
    path: "/files",
    name: "files",
    component: () => import("@/views/Files/index.vue"),
    meta: {
      title: "文件",
      keepAlive: false,
    },
  },
  {
    path: "/favorites",
    name: "favorites",
    component: () => import("@/views/Favorites/index.vue"),
    meta: {
      title: "收藏",
      keepAlive: false,
    },
  },
  {
    path: "/recycle",
    name: "recycle",
    component: () => import("@/views/Files/index.vue"),
    meta: {
      title: "回收站",
      keepAlive: false,
    },
  },
  {
    path: "/share",
    name: "share",
    component: () => import("@/views/Share/index.vue"),
    meta: {
      title: "分享",
      keepAlive: false,
    },
  },
  {
    path: "/storage",
    name: "storage",
    component: () => import("@/views/Storage/index.vue"),
    meta: {
      title: "存储",
      keepAlive: false,
    },
  },
  {
    path: "/transfer",
    name: "transfer",
    component: () => import("@/views/Transfer/index.vue"),
    meta: {
      title: "传输",
      keepAlive: false,
    },
  },
  {
    path: "/preview/:fileId",
    name: "file-preview",
    component: () => import("@/views/Preview/index.vue"),
    meta: {
      title: "文件预览",
      layout: false,
    },
  },
  {
    path: "/s/:shareId",
    name: "share-public",
    component: () => import("@/views/SharePublic/index.vue"),
    meta: {
      title: "公开分享",
      layout: false,
    },
  },
  {
    path: "/:pathMatch(.*)*",
    name: "not-found",
    component: () => import("@/views/not-found/index.vue"),
    meta: {
      title: "404",
      layout: false,
    },
  },
];
