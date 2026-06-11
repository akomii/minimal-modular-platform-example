import { createRouter, createWebHistory } from "vue-router"
import ManagementView from "../components/ManagementView.vue"

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/",
      name: "management",
      component: ManagementView
    }
  ]
})

export default router
