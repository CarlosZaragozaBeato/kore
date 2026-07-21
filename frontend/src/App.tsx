import { useState } from 'react'
import { clearUsername, getUsername } from './session'
import Login from './pages/Login'
import Home from './pages/Home'

export default function App() {
  const [username, setUsernameState] = useState<string | null>(getUsername())

  if (!username) {
    return <Login onLogin={setUsernameState} />
  }

  return (
    <Home
      username={username}
      onLogout={() => {
        clearUsername()
        setUsernameState(null)
      }}
    />
  )
}
