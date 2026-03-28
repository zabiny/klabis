import {Link as RouterLink} from 'react-router-dom'
import {Users, Calendar, Layers, Activity, User, ChevronRight} from 'lucide-react'
import {Card} from '../components/UI'
import {useRootNavigation} from "../hooks/useRootNavigation";
import {useIsAdmin} from "../hooks/useIsAdmin";
import {useAuth} from "../contexts/AuthContext2";
import {labels} from "../localization/labels";
import {mockStats, mockUpcomingEvents, mockMyEvents} from "./dashboard/mockDashboardData";
import {formatDate} from "../utils/dateUtils";

const navigationCards = [
    {
        rel: 'members',
        title: 'Členové',
        description: 'Spravujte členy klubu, jejich údaje, registrace a oprávnění. Prohlížejte aktivní členy, sledujte členské příspěvky a historii.',
        icon: (
            <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={1.5}>
                <path strokeLinecap="round" strokeLinejoin="round"
                      d="M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z"/>
            </svg>
        ),
        gradient: 'from-emerald-500 to-teal-600',
    },
    {
        rel: 'events',
        title: 'Akce',
        description: 'Prohlížejte nadcházející akce, závody, tréninky a kempy. Sledujte historii akcí, spravujte účast a plánujte nové eventy.',
        icon: (
            <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={1.5}>
                <path strokeLinecap="round" strokeLinejoin="round"
                      d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 11.25v7.5"/>
            </svg>
        ),
        gradient: 'from-blue-500 to-indigo-600',
    },
    {
        rel: 'groups',
        title: 'Skupiny',
        description: 'Organizujte členy do skupin, tréninků a týmů. Spravujte skupinové údaje, sledujte členství a spravujte skupinové aktivity.',
        icon: (
            <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={1.5}>
                <path strokeLinecap="round" strokeLinejoin="round"
                      d="M18 18.72a9.094 9.094 0 003.741-.479 3 3 0 00-4.682-2.72m.94 3.198l.001.031c0 .225-.012.447-.037.666A11.944 11.944 0 0112 21c-2.17 0-4.207-.576-5.963-1.584A6.062 6.062 0 016 18.719m12 0a5.971 5.971 0 00-.941-3.197m0 0A5.995 5.995 0 0012 12.75a5.995 5.995 0 00-5.058 2.772m0 0a3 3 0 00-4.681 2.72 8.986 8.986 0 003.74.477m.94-3.197a5.971 5.971 0 00-.94 3.197M15 6.75a3 3 0 11-6 0 3 3 0 016 0zm6 3a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0zm-13.5 0a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0z"/>
            </svg>
        ),
        gradient: 'from-violet-500 to-purple-600',
    },
    {
        rel: 'admin',
        title: 'Administrace',
        description: 'Přejděte celou aplikaci pomocí generického HAL Navigatoru. Pracujte se všemi dostupnými endpointy, prozkoumejte HAL strukturu.',
        icon: (
            <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={1.5}>
                <path strokeLinecap="round" strokeLinejoin="round"
                      d="M9.594 3.94c.09-.542.56-.94 1.11-.94h2.593c.55 0 1.02.398 1.11.94l.213 1.281c.063.374.313.686.645.87.074.04.147.083.22.127.324.196.72.257 1.075.124l1.217-.456a1.125 1.125 0 011.37.49l1.296 2.247a1.125 1.125 0 01-.26 1.431l-1.003.827c-.293.24-.438.613-.431.992a6.759 6.759 0 010 .255c-.007.378.138.75.43.99l1.005.828c.424.35.534.954.26 1.43l-1.298 2.247a1.125 1.125 0 01-1.369.491l-1.217-.456c-.355-.133-.75-.072-1.076.124a6.57 6.57 0 01-.22.128c-.331.183-.581.495-.644.869l-.213 1.28c-.09.543-.56.941-1.11.941h-2.594c-.55 0-1.02-.398-1.11-.94l-.213-1.281c-.062-.374-.312-.686-.644-.87a6.52 6.52 0 01-.22-.127c-.325-.196-.72-.257-1.076-.124l-1.217.456a1.125 1.125 0 01-1.369-.49l-1.297-2.247a1.125 1.125 0 01.26-1.431l1.004-.827c.292-.24.437-.613.43-.992a6.932 6.932 0 010-.255c.007-.378-.138-.75-.43-.99l-1.004-.828a1.125 1.125 0 01-.26-1.43l1.297-2.247a1.125 1.125 0 011.37-.491l1.216.456c.356.133.751.072 1.076-.124.072-.044.146-.087.22-.128.332-.183.582-.495.644-.869l.214-1.281z"/>
                <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
            </svg>
        ),
        gradient: 'from-amber-500 to-orange-600',
    },
]

const containsRel = (menuItems: { rel: string }[], rel: string) => menuItems.some(item => item.rel === rel)

const AdminDashboard = ({firstName, menuItems}: { firstName: string; menuItems: { rel: string }[] }) => {

    return (
        <div className="space-y-8 animate-fade-in">
            <div>
                <h1 className="font-display text-5xl lg:text-6xl font-bold text-gradient-primary mb-3">
                    {labels.dashboard.welcome}, {firstName}
                </h1>
                <p className="text-lg text-text-secondary max-w-3xl">
                    {labels.dashboard.subtitle}
                </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <div className="card p-5">
                    <div className="flex items-center justify-between mb-3">
                        <div className="p-2 bg-emerald-100 dark:bg-emerald-900/30 rounded-lg">
                            <Users className="w-5 h-5 text-emerald-600 dark:text-emerald-400"/>
                        </div>
                    </div>
                    <div className="text-3xl font-bold text-text-primary mb-1">{mockStats.activeMembersCount}</div>
                    <div className="text-sm font-medium text-text-secondary">{labels.dashboard.activeMembers}</div>
                </div>

                <div className="card p-5">
                    <div className="flex items-center justify-between mb-3">
                        <div className="p-2 bg-blue-100 dark:bg-blue-900/30 rounded-lg">
                            <Calendar className="w-5 h-5 text-blue-600 dark:text-blue-400"/>
                        </div>
                    </div>
                    <div className="text-3xl font-bold text-text-primary mb-1">{mockStats.upcomingEventsCount}</div>
                    <div className="text-sm font-medium text-text-secondary">{labels.dashboard.upcomingEvents}</div>
                </div>

                <div className="card p-5">
                    <div className="flex items-center justify-between mb-3">
                        <div className="p-2 bg-violet-100 dark:bg-violet-900/30 rounded-lg">
                            <Layers className="w-5 h-5 text-violet-600 dark:text-violet-400"/>
                        </div>
                    </div>
                    <div className="text-3xl font-bold text-text-primary mb-1">{mockStats.totalGroupsCount}</div>
                    <div className="text-sm font-medium text-text-secondary">{labels.dashboard.groups}</div>
                </div>

                <div className="card p-5">
                    <div className="flex items-center justify-between mb-3">
                        <div className="p-2 bg-green-100 dark:bg-green-900/30 rounded-lg">
                            <Activity className="w-5 h-5 text-green-600 dark:text-green-400"/>
                        </div>
                    </div>
                    <div className="text-3xl font-bold text-success mb-1">{labels.dashboard.online}</div>
                    <div className="text-sm font-medium text-text-secondary">{labels.dashboard.systemStatus}</div>
                </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {navigationCards.map((card) => (
                    containsRel(menuItems, card.rel) && (
                        <RouterLink
                            key={card.rel}
                            to={card.rel === 'admin' ? '/sandplace' : `/${card.rel}`}
                            className="group block"
                        >
                            <Card className="card-hoverable h-full p-5">
                                <div className="flex items-center gap-4">
                                    <div className={`p-3 rounded-xl bg-gradient-to-br ${card.gradient} text-white shrink-0`}>
                                        <div className="w-6 h-6 [&>svg]:w-6 [&>svg]:h-6">{card.icon}</div>
                                    </div>
                                    <div className="flex-1">
                                        <p className="font-semibold text-text-primary">{card.title}</p>
                                        <p className="text-sm text-text-secondary line-clamp-1">{card.description}</p>
                                    </div>
                                    <ChevronRight className="w-5 h-5 text-text-tertiary group-hover:translate-x-1 transition-transform"/>
                                </div>
                            </Card>
                        </RouterLink>
                    )
                ))}
            </div>

            <div>
                <h2 className="text-2xl font-display font-bold text-text-primary mb-4">
                    {labels.dashboard.upcomingEvents}
                </h2>
                <Card className="overflow-hidden">
                    <div className="divide-y divide-border">
                        {mockUpcomingEvents.map((event) => (
                            <div key={event.id} className="flex items-center justify-between p-4 hover:bg-surface-hover transition-colors">
                                <div className="flex items-center gap-4">
                                    <div className="p-2 bg-blue-100 dark:bg-blue-900/30 rounded-lg shrink-0">
                                        <Calendar className="w-4 h-4 text-blue-600 dark:text-blue-400"/>
                                    </div>
                                    <div>
                                        <p className="font-medium text-text-primary">{event.name}</p>
                                        <p className="text-sm text-text-secondary">{event.location} · {event.organizer}</p>
                                    </div>
                                </div>
                                <div className="flex items-center gap-3 shrink-0">
                                    <span className="text-sm text-text-secondary hidden sm:block">
                                        {formatDate(event.eventDate)}
                                    </span>
                                    <ChevronRight className="w-4 h-4 text-text-tertiary"/>
                                </div>
                            </div>
                        ))}
                    </div>
                </Card>
            </div>
        </div>
    )
}

const UserDashboard = ({firstName, memberId, menuItems}: {
    firstName: string;
    memberId: string | null;
    menuItems: { rel: string }[]
}) => {
    return (
        <div className="space-y-8 animate-fade-in">
            <div>
                <h1 className="font-display text-5xl lg:text-6xl font-bold text-gradient-primary mb-3">
                    {labels.dashboard.welcome}, {firstName}
                </h1>
                <p className="text-lg text-text-secondary max-w-3xl">
                    {labels.dashboard.subtitle}
                </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {memberId && (
                    <RouterLink to={`/members/${memberId}`} className="group block">
                        <Card className="card-hoverable h-full p-5">
                            <div className="flex items-center gap-4">
                                <div className="p-3 bg-emerald-100 dark:bg-emerald-900/30 rounded-xl">
                                    <User className="w-6 h-6 text-emerald-600 dark:text-emerald-400"/>
                                </div>
                                <div className="flex-1">
                                    <p className="font-semibold text-text-primary">{labels.dashboard.myProfile}</p>
                                    <p className="text-sm text-text-secondary">Zobrazit a upravit profil</p>
                                </div>
                                <ChevronRight className="w-5 h-5 text-text-tertiary group-hover:translate-x-1 transition-transform"/>
                            </div>
                        </Card>
                    </RouterLink>
                )}

                {containsRel(menuItems, 'events') && (
                    <RouterLink to="/events" className="group block">
                        <Card className="card-hoverable h-full p-5">
                            <div className="flex items-center gap-4">
                                <div className="p-3 bg-blue-100 dark:bg-blue-900/30 rounded-xl">
                                    <Calendar className="w-6 h-6 text-blue-600 dark:text-blue-400"/>
                                </div>
                                <div className="flex-1">
                                    <p className="font-semibold text-text-primary">Závody</p>
                                    <p className="text-sm text-text-secondary">Prohlížet nadcházející závody</p>
                                </div>
                                <ChevronRight className="w-5 h-5 text-text-tertiary group-hover:translate-x-1 transition-transform"/>
                            </div>
                        </Card>
                    </RouterLink>
                )}
            </div>

            <div>
                <h2 className="text-2xl font-display font-bold text-text-primary mb-4">
                    {labels.dashboard.myEvents}
                </h2>
                <Card className="overflow-hidden">
                    {mockMyEvents.length > 0 ? (
                        <div className="divide-y divide-border">
                            {mockMyEvents.map((event) => (
                                <div key={event.id} className="flex items-center justify-between p-4 hover:bg-surface-hover transition-colors">
                                    <div className="flex items-center gap-4">
                                        <div className="p-2 bg-blue-100 dark:bg-blue-900/30 rounded-lg shrink-0">
                                            <Calendar className="w-4 h-4 text-blue-600 dark:text-blue-400"/>
                                        </div>
                                        <div>
                                            <p className="font-medium text-text-primary">{event.name}</p>
                                            <p className="text-sm text-text-secondary">{event.location}</p>
                                        </div>
                                    </div>
                                    <span className="text-sm text-text-secondary hidden sm:block">
                                        {formatDate(event.eventDate)}
                                    </span>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <p className="p-6 text-center text-text-secondary">{labels.dashboard.noUpcomingEvents}</p>
                    )}
                </Card>
            </div>
        </div>
    )
}

const HomePage = () => {
    const {data: menuItems = []} = useRootNavigation()
    const {isAdmin} = useIsAdmin()
    const {getUser} = useAuth()
    const user = getUser()
    const firstName = user?.firstName ?? ''
    const memberId = user?.memberId ?? null

    if (isAdmin) {
        return <AdminDashboard firstName={firstName} menuItems={menuItems}/>
    }

    return <UserDashboard firstName={firstName} memberId={memberId} menuItems={menuItems}/>
}

export default HomePage
