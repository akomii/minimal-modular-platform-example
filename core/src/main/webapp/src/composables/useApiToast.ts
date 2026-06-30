import { useToast } from "primevue/usetoast"
import { errorDetail, type ApiResponse } from "./useApi"

// Toast helpers over the shared API response, so every composable reports outcomes the same way.
export function useApiToast() {
  const toast = useToast()

  // Surfaces the server's ProblemDetail message (or the status) as an error toast under the given summary.
  function failToast(summary: string, res: ApiResponse): void {
    toast.add({ severity: "error", summary, detail: errorDetail(res), life: 6000 })
  }

  // Shows a success toast (running the optional state update) or surfaces the server's error message.
  function handle(
    res: ApiResponse,
    success: string,
    onOk?: (body: unknown) => void
  ): void {
    if (res.ok) {
      onOk?.(res.body)
      toast.add({ severity: "success", summary: success, life: 3000 })
    } else {
      failToast("Action failed", res)
    }
  }

  return { failToast, handle }
}
