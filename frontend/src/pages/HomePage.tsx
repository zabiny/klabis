import {useState} from 'react'
import {Link as RouterLink} from 'react-router-dom'
import {Bell, Calendar, ChevronRight, User, UserPen} from 'lucide-react'
import {Button, Card} from '../components/UI'
import {EventRegistrationDialog} from '../components/events/EventRegistrationDialog'
import {getActionVariant} from '../utils/actionVariants'
import {toRegistrationLink} from '../utils/halFormsUtils'
import type {Link} from '../api'
import {useRootNavigation} from "../hooks/useRootNavigation";
import {labels} from "../localization/labels";
import {formatDate} from "../utils/dateUtils";
import {useDashboard} from "../hooks/useDashboard";
import {useMyUpcomingRegistrations} from "../hooks/useMyUpcomingRegistrations";
import {extractNavigationPath} from "../utils/navigationPath";
import {UpcomingDeadlinesWidget} from "../components/dashboard/UpcomingDeadlinesWidget";

const containsRel = (menuItems: { rel: string }[], rel: string) => menuItems.some(item => item.rel === rel)

const UserDashboard = ({profileHref, menuItems}: {
    profileHref: string | null;
    menuItems: { rel: string; href?: string }[]
}) => {
    const {data: dashboardData} = useDashboard()
    const upcomingRegistrationsHref = dashboardData?.upcomingRegistrationsHref
    const upcomingDeadlinesHref = dashboardData?.upcomingDeadlinesHref
    const [registrationTarget, setRegistrationTarget] = useState<Link | null>(null)
    const {data: registrationsData, refetch: refetchRegistrations} = useMyUpcomingRegistrations(upcomingRegistrationsHref)
    const registrationItems = registrationsData?.items ?? []
    const notificationsHref = menuItems.find(item => item.rel === 'notifications')?.href ?? null

    return (
        <div className="space-y-8 animate-fade-in">
            <div className="hidden sm:grid grid-cols-1 sm:grid-cols-2 gap-4">
                {profileHref && (
                    <RouterLink to={profileHref} className="group block">
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

                {notificationsHref && (
                    <RouterLink to={notificationsHref} className="group block">
                        <Card className="card-hoverable h-full p-5">
                            <div className="flex items-center gap-4">
                                <div className="p-3 bg-amber-100 dark:bg-amber-900/30 rounded-xl">
                                    <Bell className="w-6 h-6 text-amber-600 dark:text-amber-400"/>
                                </div>
                                <div className="flex-1">
                                    <p className="font-semibold text-text-primary">{labels.dashboard.notificationsPanelTitle}</p>
                                    <p className="text-sm text-text-secondary">{labels.dashboard.notificationsPanelSubtitle}</p>
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
                                    {registrationItems.map((event) => {
                                        const editLink = toRegistrationLink(event.editRegistrationTarget)
                                        return (
                                        <RouterLink
                                            key={event.selfHref}
                                            to={extractNavigationPath(event.selfHref)}
                                            className="flex items-center justify-between p-4 hover:bg-surface-hover transition-colors"
                                        >
                                            <div className="flex items-center gap-4 flex-1 min-w-0">
                                                <div className="p-2 bg-blue-100 dark:bg-blue-900/30 rounded-lg shrink-0">
                                                    <Calendar className="w-4 h-4 text-blue-600 dark:text-blue-400"/>
                                                </div>
                                                <div className="min-w-0">
                                                    <p className="font-medium text-text-primary truncate">{event.name}</p>
                                                    <p className="text-sm text-text-secondary">{event.location}</p>
                                                </div>
                                            </div>
                                            <span className="text-sm text-text-secondary hidden sm:block ml-3 shrink-0">
                                                {formatDate(event.eventDate)}
                                            </span>
                                            {editLink && (
                                                <Button
                                                    variant={getActionVariant('editRegistration')}
                                                    size="sm"
                                                    className="ml-3 shrink-0"
                                                    title={labels.templates.editRegistration}
                                                    onClick={(e) => {
                                                        e.preventDefault()
                                                        e.stopPropagation()
                                                        setRegistrationTarget(editLink)
                                                    }}
                                                >
                                                    <UserPen className="w-4 h-4"/>
                                                </Button>
                                            )}
                                        </RouterLink>
                                        )
                                    })}
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

            <EventRegistrationDialog
                registration={registrationTarget}
                onClose={() => setRegistrationTarget(null)}
                onChanged={refetchRegistrations}
            />
        </div>
    )
}

const HomePage = () => {
    const {data: menuItems = []} = useRootNavigation()
    const profileHref = menuItems.find(item => item.rel === 'profile')?.href ?? null

    return <UserDashboard profileHref={profileHref} menuItems={menuItems}/>
}

export default HomePage
