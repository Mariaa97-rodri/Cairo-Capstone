import { createContext, useContext, useReducer, useCallback, useMemo } from 'react'
import { authService } from '../services'

const AuthContext = createContext(null)

const stored = {
  user:  JSON.parse(localStorage.getItem('cairo_user') || 'null'),
  token: localStorage.getItem('cairo_token') || null,
}

function reducer(state, action) {
  switch (action.type) {
    case 'START':   return { ...state, loading: true,  error: null }
    case 'SUCCESS': return { ...state, loading: false, error: null, ...action.payload }
    case 'ERROR':   return { ...state, loading: false, error: action.payload }
    case 'LOGOUT':  return { user: null, token: null, loading: false, error: null }
    default:        return state
  }
}

export function AuthProvider({ children }) {
  const [state, dispatch] = useReducer(reducer, {
    user: stored.user, token: stored.token, loading: false, error: null,
  })

  const persist = (data) => {
    const user = { id: data.userId, name: data.name, email: data.email, role: data.role }
    localStorage.setItem('cairo_token', data.token)
    localStorage.setItem('cairo_user',  JSON.stringify(user))
    dispatch({ type: 'SUCCESS', payload: { user, token: data.token } })
    return user
  }

  const login = useCallback(async (email, password) => {
    dispatch({ type: 'START' })
    try {
      const { data } = await authService.login(email, password)
      persist(data)
      return { success: true }
    } catch (err) {
      const msg = err.response?.data?.message || 'Login failed.'
      dispatch({ type: 'ERROR', payload: msg })
      return { success: false, error: msg }
    }
  }, [])

  const register = useCallback(async (name, email, password) => {
    dispatch({ type: 'START' })
    try {
      const { data } = await authService.register(name, email, password)
      persist(data)
      return { success: true }
    } catch (err) {
      const msg = err.response?.data?.message || 'Registration failed.'
      dispatch({ type: 'ERROR', payload: msg })
      return { success: false, error: msg }
    }
  }, [])

const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  dispatch({ type: 'LOGOUT' })
  window.location.href = '/'
}

  const value = useMemo(() => ({
    ...state,
    isAuthenticated: !!state.token,
    isAdmin: state.user?.role === 'ADMIN',
    login, register, logout,
  }), [state, login, register, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}