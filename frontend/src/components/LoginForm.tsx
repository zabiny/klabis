'use client'

import * as React from 'react'

import { Label } from '@/components/ui/label.tsx'
import { Input } from '@/components/ui/input.tsx'
import { Button } from '@/components/ui/button.tsx'
import { CgSpinner } from 'react-icons/cg'
import { VscGithubInverted } from 'react-icons/vsc'
import { signIn } from 'auth-astro/client'

export function LoginForm() {
  const [isLoading, setIsLoading] = React.useState<boolean>(false)
  const [isLoadingGithub, setIsLoadingGithub] = React.useState<boolean>(false)

  async function onSubmit(event: React.SyntheticEvent) {
    event.preventDefault()
    setIsLoading(true)

    setTimeout(() => {
      setIsLoading(false)
    }, 3000)
  }

  return (
    <form onSubmit={onSubmit}>
      <div className="grid gap-4">
        <div className="grid gap-2">
          <Label htmlFor="email">Email</Label>
          <Input
            id="email"
            placeholder="name@example.com"
            type="email"
            autoCapitalize="none"
            autoComplete="email"
            autoCorrect="off"
            disabled={isLoading}
          />
        </div>
        <div className="grid gap-2">
          <div className="flex items-center">
            <Label htmlFor="password">Password</Label>
            <a
              href="/forgot-password"
              className="ml-auto inline-block text-sm underline"
            >
              Forgot your password?
            </a>
          </div>
          <Input id="password" type="password" required />
        </div>
        <Button type="submit" className="w-full" disabled={isLoading}>
          {isLoading && <CgSpinner className="mr-2 h-4 w-4 animate-spin" />}
          Login
        </Button>
        <div className="relative">
          <div className="absolute inset-0 flex items-center">
            <span className="w-full border-t" />
          </div>
          <div className="relative flex justify-center text-xs uppercase">
            <span className="bg-background px-2 text-muted-foreground">
              Or continue with
            </span>
          </div>
        </div>
        <Button
          type="button"
          variant="outline"
          className="w-full"
          disabled={isLoadingGithub}
          onClick={() => {
            signIn('github')
            setIsLoadingGithub(true)
          }}
        >
          {isLoadingGithub ? (
            <CgSpinner className="mr-2 h-4 w-4 animate-spin" />
          ) : (
            <VscGithubInverted className="mr-2 h-4 w-4" />
          )}{' '}
          GitHub
        </Button>
      </div>
    </form>
  )
}
