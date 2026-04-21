import { useState, useCallback } from 'react'

export function useApi() {
  const [loading, setLoading] = useState(false)
  const [error,   setError]   = useState(null)

  const execute = useCallback(async (apiCall, onSuccess) => {
    setLoading(true)
    setError(null)
    try {
      const result = await apiCall()
      if (onSuccess) onSuccess(result.data)
      return { success: true, data: result.data }
    } catch (err) {
      const msg = err.response?.data?.message || 'Something went wrong.'
      setError(msg)
      return { success: false, error: msg }
    } finally {
      setLoading(false)
    }
  }, [])

  return { loading, error, execute, setError }
}