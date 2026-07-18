import { Link } from 'react-router-dom'
import { Wordmark } from '@/components/Wordmark.tsx'

interface AuthPageProps {
  mode: 'signin' | 'signup'
}

export function AuthPage({ mode }: AuthPageProps) {
  const isSignin = mode === 'signin'

  return (
    <div className="flex min-h-svh flex-col">
      <header className="mx-auto flex w-full max-w-6xl items-center px-6 py-5">
        <Wordmark />
      </header>
      <main className="flex flex-1 items-center justify-center px-6 pb-24">
        <div className="w-full max-w-sm rounded-lg border border-border bg-card p-8 shadow-sm">
          <h1 className="font-stretch-expanded text-xl font-black uppercase">
            {isSignin ? 'Sign in' : 'Create your account'}
          </h1>
          <p className="mt-3 text-sm text-muted-foreground">
            Accounts aren't open yet — sign in and sign up land in the next release.
          </p>
          <p className="mt-6 border-t border-border pt-4 text-sm">
            {isSignin ? (
              <>
                New to Liftform?{' '}
                <Link
                  to="/register"
                  className="rounded-sm font-medium underline underline-offset-4 hover:text-plate-red focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring"
                >
                  Create an account
                </Link>
              </>
            ) : (
              <>
                Already have an account?{' '}
                <Link
                  to="/login"
                  className="rounded-sm font-medium underline underline-offset-4 hover:text-plate-red focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring"
                >
                  Sign in
                </Link>
              </>
            )}
          </p>
        </div>
      </main>
    </div>
  )
}
