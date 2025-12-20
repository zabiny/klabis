import {Link as RouterLink} from 'react-router-dom'
import {Button, Card} from '../components/UI'

/**
 * HomePage - Main dashboard/home page
 * Displays welcome message and navigation cards to main sections
 */
const HomePage = () => {
  return (
      <div className="space-y-6">
        {/* Header Section */}
        <div>
          <h1 className="text-4xl font-bold text-gray-900 dark:text-white mb-2">
            Vítejte v členské sekci
          </h1>
          <p className="text-lg text-gray-600 dark:text-gray-400">
            Vítejte v členské sekci klubu orientačního běhu. Zde můžete spravovat své údaje, prohlížet akce a
            skupiny.
          </p>
        </div>

        {/* Feature Cards Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-8">
          {/* Members Card */}
          <Card hoverable className="p-6 flex flex-col h-full">
            <div className="flex items-center gap-3 mb-4">
              <div className="flex-shrink-0">
                <svg
                    className="w-8 h-8 text-primary"
                    fill="currentColor"
                    viewBox="0 0 20 20"
                >
                  <path d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z"/>
                </svg>
              </div>
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
                Členové
              </h2>
            </div>
            <p className="text-gray-600 dark:text-gray-400 flex-grow mb-4">
              Prohlížejte a spravujte členy klubu, jejich údaje a oprávnění.
            </p>
            <RouterLink to="/members" className="block">
              <Button variant="primary" className="w-full">
                Zobrazit členy
              </Button>
            </RouterLink>
          </Card>

          {/* Events Card */}
          <Card hoverable className="p-6 flex flex-col h-full">
            <div className="flex items-center gap-3 mb-4">
              <div className="flex-shrink-0">
                <svg
                    className="w-8 h-8 text-primary"
                    fill="currentColor"
                    viewBox="0 0 20 20"
                >
                  <path
                      fillRule="evenodd"
                      d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z"
                      clipRule="evenodd"
                  />
                </svg>
              </div>
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
                Akce
              </h2>
            </div>
            <p className="text-gray-600 dark:text-gray-400 flex-grow mb-4">
              Prohlížejte nadcházející akce, závody a tréninky.
            </p>
            <RouterLink to="/events" className="block">
              <Button variant="primary" className="w-full">
                Zobrazit akce
              </Button>
            </RouterLink>
          </Card>

          {/* Groups Card */}
          <Card hoverable className="p-6 flex flex-col h-full">
            <div className="flex items-center gap-3 mb-4">
              <div className="flex-shrink-0">
                <svg
                    className="w-8 h-8 text-primary"
                    fill="currentColor"
                    viewBox="0 0 20 20"
                >
                  <path
                      d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v2h8v-2zM6 8a2 2 0 11-4 0 2 2 0 014 0zM16 18v-2a4 4 0 00-8 0v2h8zM9 12a4 4 0 100-8 4 4 0 000 8z"/>
                </svg>
              </div>
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
                Skupiny
              </h2>
            </div>
            <p className="text-gray-600 dark:text-gray-400 flex-grow mb-4">
              Prohlížejte a spravujte skupiny členů, tréninkové skupiny a další.
            </p>
            <RouterLink to="/groups" className="block">
              <Button variant="primary" className="w-full">
                Zobrazit skupiny
              </Button>
            </RouterLink>
          </Card>
        </div>
      </div>
  )
}

export default HomePage