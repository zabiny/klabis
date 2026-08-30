import {Link as RouterLink} from 'react-router-dom'
import {Calendar, ChevronRight, User} from 'lucide-react'
import {Card} from '../components/UI'
import {useRootNavigation} from "../hooks/useRootNavigation";
import {useAuth} from "../contexts/authContext";
import {labels} from "../localization/labels";
import {formatDate} from "../utils/dateUtils";
import {useDashboard} from "../hooks/useDashboard";
import {useMyUpcomingRegistrations} from "../hooks/useMyUpcomingRegistrations";
import {extractNavigationPath} from "../utils/navigationPath";
import {UpcomingDeadlinesWidget} from "../components/dashboard/UpcomingDeadlinesWidget";

const containsRel = (menuItems: { rel: string }[], rel: string) => menuItems.some(item => item.rel === rel)

const UserDashboard = ({memberId, menuItems}: {
    memberId: string | null;
    menuItems: { rel: string }[]
}) => {
    const {data: dashboardData} = useDashboard()
    const upcomingRegistrationsHref = dashboardData?.upcomingRegistrationsHref
    const upcomingDeadlinesHref = dashboardData?.upcomingDeadlinesHref
    const {data: registrationsData} = useMyUpcomingRegistrations(upcomingRegistrationsHref)
    const registrationItems = registrationsData?.items ?? []

    return (
        <div className="space-y-8 animate-fade-in">
            <div className="hidden sm:grid grid-cols-1 sm:grid-cols-2 gap-4">
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
                                    <p className="font-semibold text-text-primary">{labels.dashboard.eventsPanelTitle}</p>
                                    <p className="text-sm text-text-secondary">{labels.dashboard.eventsPanelSubtitle}</p>
                                </div>
                                <ChevronRight className="w-5 h-5 text-text-tertiary group-hover:translate-x-1 transition-transform"/>
                            </div>
                        </Card>
                    </RouterLink>
                )}
            </div>

            {upcomingRegistrationsHref && (
                <div>
                    <h2 className="text-2xl font-display font-bold text-text-primary mb-4">
                        {labels.dashboard.myEvents}
                    </h2>
                    <Card className="overflow-hidden">
                        {registrationItems.length > 0 ? (
                            <>
                                <div className="divide-y divide-border">
                                    {registrationItems.map((event) => (
                                        <RouterLink
                                            key={event.selfHref}
                                            to={extractNavigationPath(event.selfHref)}
                                            className="flex items-center justify-between p-4 hover:bg-surface-hover transition-colors"
                                        >
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
                                        </RouterLink>
                                    ))}
                                </div>
                                <div className="p-4 border-t border-border">
                                    <RouterLink
                                        to="/events?registeredBy=me&time=budouci"
                                        className="text-sm font-medium text-primary hover:underline"
                                    >
                                        {labels.dashboard.showAll}
                                    </RouterLink>
                                </div>
                            </>
                        ) : (
                            <div className="p-6 text-center">
                                <p className="text-text-secondary mb-3">{labels.dashboard.noUpcomingEvents}</p>
                                <RouterLink
                                    to="/events?time=budouci"
                                    className="text-sm font-medium text-primary hover:underline"
                                >
                                    {labels.dashboard.browseClubEvents}
                                </RouterLink>
                            </div>
                        )}
                    </Card>
                </div>
            )}

            <UpcomingDeadlinesWidget upcomingDeadlinesHref={upcomingDeadlinesHref}/>
        </div>
    )
}

const HomePage = () => {
    const {data: menuItems = []} = useRootNavigation()
    const {getUser} = useAuth()
    const user = getUser()
    const memberId = user?.memberId ?? null

    return <UserDashboard memberId={memberId} menuItems={menuItems}/>
}

export default HomePage
