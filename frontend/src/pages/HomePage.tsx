import {Link as RouterLink} from 'react-router-dom'
import {Button, Card} from '../components/UI'
import {useRootNavigation} from "../hooks/useRootNavigation";

/**
 * HomePage - Main dashboard/home page
 * Displays welcome message and navigation cards to main sections
 */
const HomePage = () => {
    const {data: menuItems = []} = useRootNavigation()

    // Helper function to check if menu contains a rel
    const containsMainMenuRel = (rel: string): boolean => {
        return menuItems?.some((item: {rel: string}) => item.rel === rel) ?? false
    }

    const navigationCards = [
        {
            rel: 'members',
            title: 'Členové',
            description: 'Spravujte členy klubu, jejich údaje, registrace a oprávnění. Prohlížejteaktivní členy, sledujte členské příspěvky a historii.',
            icon: (
                <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={1.5}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M17 20h5v-8a1 1 0 00-1-1V6a1 1 0 00-1-1H6a1 1 0 00-1 1v12a1 1 0 001 1h12a1 1 0 001-1V6a1 1 0 00-1-1h-1zm0-6a4 4 0 00-8 0m8 0a4 4 0 00-8 8m-8-8a4 4 0 008 0z"/>
                </svg>
            ),
            gradient: 'from-emerald-500 to-teal-600',
            darkGradient: 'from-emerald-400 to-teal-500',
        },
        {
            rel: 'events',
            title: 'Akce',
            description: 'Prohlížejte nadcházející akce, závody, tréninky a kempy. Sledujtehistorii akcí, spravujte účast a plánejte nové eventy.',
            icon: (
                <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={1.5}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M8 7V3m8 4V3m-9 18h18M3 21h18M3 3v18m0-4-4 4-4m12 0v12m0 4-4-4"/>
                </svg>
            ),
            gradient: 'from-blue-500 to-indigo-600',
            darkGradient: 'from-blue-400 to-indigo-500',
        },
        {
            rel: 'groups',
            title: 'Skupiny',
            description: 'Organizujte členy do skupin, tréninků a týmů. Spravujteskupinové údaje, sledujte členství a spravujte skupinové aktivity.',
            icon: (
                <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={1.5}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M17 20h5v-8a1 1 0 00-1-1V6a1 1 0 00-1-1H6a1 1 0 00-1 1v12a1 1 0 001 1h12a1 1 0 001-1V6a1 1 0 00-1-1h-1zm0-6a4 4 0 00-8 0m8 0a4 4 0 00-8 8m-8-8a4 4 0 008 0zm9 2a7 7 0 11-14 0 7 7 0 0114 0z"/>
                </svg>
            ),
            gradient: 'from-violet-500 to-purple-600',
            darkGradient: 'from-violet-400 to-purple-500',
        },
        {
            rel: 'admin',
            title: 'Administrace',
            description: 'Přejděte celou aplikaci pomoci generickeho HalNavigatoru. Pracujte všechny dostupné endpointy, prozkoumejte HAL strukturu.',
            icon: (
                <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={1.5}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37-.826a1.724 1.724 0 01-2.573 1.066c0 .956-.076 2.227-.826 2.37-.826 1.724 1.724 0 01-1.066 2.573c-.94.426-2.099 1.311-3.35.826M8.666 16.5c-1.756.426-2.924 1.756-3.35 0a1.724 1.724 0 01-1.066-2.573c-.956.94-.826 2.227-.826 2.37 1.724 1.724 0 01-1.066 2.573.94.426 1.311 2.099.826 3.35"/>
                </svg>
            ),
            gradient: 'from-amber-500 to-orange-600',
            darkGradient: 'from-amber-400 to-orange-500',
        },
    ]

    return (
        <div className="space-y-8 animate-fade-in">
            {/* Header Section with gradient text */}
            <div className="space-y-4">
                <div>
                    <h1 className="font-display text-5xl lg:text-6xl font-bold text-gradient-primary mb-3">
                        Vítejte v Klabis
                    </h1>
                    <p className="text-lg text-text-secondary max-w-3xl">
                        Moderní systém pro správu členského klubu. Spravujte členy, akce, skupiny a
                        další údaje v intuitive a efektivním rozhraní.
                    </p>
                </div>

                {/* Quick stats / info cards */}
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                    <div className="card p-4">
                        <div className="text-sm font-medium text-text-secondary mb-1">
                            Aktivních členů
                        </div>
                        <div className="text-2xl font-bold text-primary">
                            {containsMainMenuRel('members') ? 'Dostupné' : 'Nedostupné'}
                        </div>
                    </div>
                    <div className="card p-4">
                        <div className="text-sm font-medium text-text-secondary mb-1">
                            Nadcházející akce
                        </div>
                        <div className="text-2xl font-bold text-secondary">
                            {containsMainMenuRel('events') ? 'Dostupné' : 'Nedostupné'}
                        </div>
                    </div>
                    <div className="card p-4">
                        <div className="text-sm font-medium text-text-secondary mb-1">
                            Skupiny a týmy
                        </div>
                        <div className="text-2xl font-bold text-accent">
                            {containsMainMenuRel('groups') ? 'Dostupné' : 'Nedostupné'}
                        </div>
                    </div>
                    <div className="card p-4">
                        <div className="text-sm font-medium text-text-secondary mb-1">
                            Systémový status
                        </div>
                        <div className="text-2xl font-bold text-success">
                            Online
                        </div>
                    </div>
                </div>
            </div>

            {/* Navigation Cards Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mt-8">
                {navigationCards.map((card) => (
                    containsMainMenuRel(card.rel) && (
                        <RouterLink
                            key={card.rel}
                            to={card.rel === 'admin' ? '/sandplace' : `/${card.rel}`}
                            className="group block"
                        >
                            <Card className="card-hoverable h-full overflow-hidden relative">
                                {/* Gradient icon background */}
                                <div className={`
                                    absolute top-0 right-0 w-32 h-32 opacity-10
                                    bg-gradient-to-br ${card.gradient}
                                    dark:${card.darkGradient}
                                    blur-3xl rounded-full -translate-y-1/2 translate-x-1/2
                                    group-hover:opacity-20 transition-opacity duration-base
                                `}>
                                </div>

                                <div className="relative">
                                    {/* Icon with gradient */}
                                    <div className={`
                                        inline-flex items-center justify-center w-14 h-14 rounded-xl
                                        bg-gradient-to-br ${card.gradient}
                                                                        dark:${card.darkGradient}
                                                                        text-white shadow-lg mb-4
                                                                        group-hover:scale-110 transition-transform duration-base
                                    `}>
                                        {card.icon}
                                    </div>

                                    {/* Content */}
                                    <h2 className="text-2xl font-display font-bold text-text-primary mb-3">
                                        {card.title}
                                    </h2>
                                    <p className="text-text-secondary mb-6 line-clamp-3">
                                        {card.description}
                                    </p>

                                    {/* Action button */}
                                    <Button variant="primary" className="w-full">
                                        Otevřít {card.title.toLowerCase()}
                                    </Button>
                                </div>
                            </Card>
                        </RouterLink>
                    )
                ))}
            </div>
        </div>
    )
}

export default HomePage
