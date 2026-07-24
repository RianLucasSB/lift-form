import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import './index.css'
import { AuthProvider } from './features/auth/context/AuthContext.tsx'
import { LandingPage } from './pages/landing/LandingPage.tsx'
import { AuthPage } from './pages/auth/AuthPage.tsx'
import { OverviewPage } from './pages/overview/OverviewPage.tsx'
import { AuthRoute } from './routes/AuthRoute.tsx'
import { ProtectedRoute } from './routes/ProtectedRoute.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route element={<AuthRoute />}>
            <Route path="/login" element={<AuthPage mode="signin" />} />
            <Route path="/register" element={<AuthPage mode="signup" />} />
          </Route>
          <Route element={<ProtectedRoute />}>
            <Route path="/overview" element={<OverviewPage />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
)
