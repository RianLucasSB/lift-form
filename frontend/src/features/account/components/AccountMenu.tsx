import { Link } from 'react-router-dom'
import { LogOut, User } from 'lucide-react'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { useAuth } from '@/features/auth/context/AuthContext'
import { useAccount } from '../context/AccountContext'

export function AccountMenu() {
  const { signOut } = useAuth()
  const { currentUser } = useAccount()

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button
          type="button"
          aria-label="Account menu"
          className="flex size-8 items-center justify-center rounded-full bg-steel font-mono text-sm font-medium text-white outline-hidden focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"
        >
          {currentUser ? currentUser.username.charAt(0).toUpperCase() : <User className="size-4" />}
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuLabel className="flex flex-col gap-0.5 font-normal">
          <span className="truncate text-sm font-medium">{currentUser?.username ?? 'Loading…'}</span>
          <span className="truncate text-xs text-muted-foreground">{currentUser?.email}</span>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem asChild>
          <Link to="/account">
            <User />
            Account
          </Link>
        </DropdownMenuItem>
        <DropdownMenuItem
          onSelect={() => {
            void signOut()
          }}
        >
          <LogOut />
          Log out
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
