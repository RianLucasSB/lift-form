import { AppHeader } from '@/components/AppHeader'
import { useAccount } from '@/features/account/context/AccountContext'

export function AccountPage() {
  const { currentUser, isLoading } = useAccount()

  return (
    <div className="flex min-h-svh flex-col">
      <AppHeader />
      <main className="mx-auto w-full max-w-6xl flex-1 px-6 pb-24">
        <h1 className="font-stretch-expanded text-2xl font-black uppercase">Account</h1>
        <p className="mt-3 text-sm text-muted-foreground">Your account information.</p>

        <div className="mt-8 max-w-md rounded-lg border border-border bg-card p-6">
          {isLoading && !currentUser ? (
            <p className="text-sm text-muted-foreground">Loading…</p>
          ) : (
            <dl className="space-y-4">
              <div>
                <dt className="font-mono text-xs tracking-wide text-muted-foreground uppercase">Username</dt>
                <dd className="mt-1 text-sm font-medium">{currentUser?.username}</dd>
              </div>
              <div>
                <dt className="font-mono text-xs tracking-wide text-muted-foreground uppercase">Email</dt>
                <dd className="mt-1 text-sm font-medium">{currentUser?.email}</dd>
              </div>
            </dl>
          )}
        </div>
      </main>
    </div>
  )
}
