# Page Implementation Examples

Real page implementations from the Klabis frontend, demonstrating common patterns.

## Pattern 1: Collection List Page (EventsPage)

Simple list page with sortable table and row navigation.

```tsx
// frontend/src/pages/events/EventsPage.tsx
import {type ReactElement} from "react";
import type {EntityModel} from "../../api";
import {TableCell} from "../../components/KlabisTable";
import {HalEmbeddedTable} from "../../components/HalNavigator2/HalEmbeddedTable.tsx";
import {formatDate} from "../../utils/dateUtils.ts";
import {useHalPageData} from "../../hooks/useHalPageData.ts";

interface EventListData extends EntityModel<{
    id: string,
    name: string,
    eventDate: string,
    location: string,
    organizer: string,
    status: 'DRAFT' | 'ACTIVE' | 'FINISHED' | 'CANCELLED'
}> {}

export const EventsPage = (): ReactElement => {
    const {route} = useHalPageData();

    return <div className="flex flex-col gap-8">
        <h1 className="text-3xl font-bold text-text-primary">Zavody</h1>
        <div className="flex flex-col gap-4">
            <h2 className="text-xl font-bold text-text-primary">Seznam zavodu</h2>
            <HalEmbeddedTable<EventListData>
                collectionName={"eventSummaryDtoList"}
                defaultOrderBy={"eventDate"}
                onRowClick={route.navigateToResource}
            >
                <TableCell sortable column={"eventDate"}
                    dataRender={({value}) => typeof value === 'string' ? formatDate(value) : ''}
                >Datum</TableCell>
                <TableCell sortable column={"name"}>Nazev</TableCell>
                <TableCell sortable column={"location"}>Misto</TableCell>
                <TableCell sortable column={"organizer"}>Poradatel</TableCell>
                <TableCell sortable column={"status"}>Status</TableCell>
            </HalEmbeddedTable>
        </div>
    </div>;
}
```

## Pattern 2: List + Form Button (MembersPage)

List page with create action using multi-step form modal.

```tsx
// frontend/src/pages/members/MembersPage.tsx
import {type ReactElement} from "react";
import type {EntityModel} from "../../api";
import {TableCell} from "../../components/KlabisTable";
import {HalLinksSection} from "../../components/HalNavigator2/HalLinksSection.tsx";
import {HalEmbeddedTable} from "../../components/HalNavigator2/HalEmbeddedTable.tsx";
import {useHalPageData} from "../../hooks/useHalPageData.ts";
import {HalFormButton} from "../../components/HalNavigator2/HalFormButton.tsx";
import {type FormStep, MultiStepFormModal} from "../../components/HalNavigator2/MultiStepFormModal.tsx";

type MemberListData = EntityModel<{
    id: string,
    firstName: string,
    lastName: string,
    registrationNumber: string
}>;

export const MembersPage = (): ReactElement => {
    const {route} = useHalPageData();

    return <div className="flex flex-col gap-8">
        <h1 className="text-3xl font-bold text-text-primary">Adresar</h1>
        <div className="flex flex-col gap-4">
            <div className="flex items-center justify-between">
                <h2 className="text-xl font-bold text-text-primary">Clenove</h2>
                <RegisterMemberFormButton/>
            </div>
            <HalEmbeddedTable<MemberListData>
                collectionName={"memberSummaryResponseList"}
                defaultOrderBy={"lastName"}
                onRowClick={route.navigateToResource}
            >
                <TableCell sortable column="firstName">Jmeno</TableCell>
                <TableCell sortable column="lastName">Prijmeni</TableCell>
                <TableCell sortable column="registrationNumber">Registracni cislo</TableCell>
                <TableCell column="_links"
                    dataRender={props => (<HalLinksSection links={props.value as any}/>)}
                >Akce</TableCell>
            </HalEmbeddedTable>
        </div>
    </div>;
}

const memberRegistrationSteps: FormStep[] = [
    {
        title: 'Krok 1: Osobni udaje',
        fields: ['firstName', 'lastName', 'sex', 'dateOfBirth', 'birthCertificateNumber', 'nationality'],
    },
    {
        title: 'Krok 2: Kontaktni informace',
        fields: ['address', 'contact', 'guardians'],
    },
    {
        title: 'Krok 3: Udaje clena',
        fields: ['siCard', 'bankAccount', 'registrationNumber', 'orisId'],
    },
];

const RegisterMemberFormButton = () => {
    return <HalFormButton
        name={"memberRegistrationsPost"}
        customLayout={<MultiStepFormModal steps={memberRegistrationSteps}/>}
    />;
}
```

## Pattern 3: Detail Page with Subresources (FinancesPage)

Page displaying resource data with nested subresources via HalSubresourceProvider.

```tsx
// frontend/src/pages/finances/FinancesPage.tsx
import {type ReactElement} from "react";
import {TableCell} from "../../components/KlabisTable";
import {HalEmbeddedTable} from "../../components/HalNavigator2/HalEmbeddedTable.tsx";
import {HalSubresourceProvider, useHalRoute} from "../../contexts/HalRouteContext.tsx";
import {Skeleton} from "../../components/UI";
import {HalFormButton} from "../../components/HalNavigator2/HalFormButton.tsx";
import {useHalPageData} from "../../hooks/useHalPageData.ts";

export const MemberFinancePage = (): ReactElement => {
    const {isLoading, resourceData, route} = useHalPageData();

    if (isLoading) return <Skeleton/>

    return <div className="flex flex-col gap-8">
        <h1 className="text-3xl font-bold text-text-primary">Finance</h1>

        {/* Direct resource data display */}
        <div className="bg-surface-raised rounded-md border border-border p-6">
            <div className="flex flex-col gap-4">
                <div>
                    <p className="text-xs uppercase font-semibold text-text-secondary mb-2">Zustatek</p>
                    <p className="text-2xl font-semibold text-text-primary">
                        {resourceData?.balance as number ?? '-'} Kc
                    </p>
                </div>
                {/* Subresource: fetch owner via link */}
                <div>
                    <p className="text-xs uppercase font-semibold text-text-secondary mb-2">Majitel</p>
                    <HalSubresourceProvider subresourceLinkName={"owner"}>
                        <MemberName/>
                    </HalSubresourceProvider>
                </div>
            </div>
        </div>

        {/* Subresource table: transactions fetched via link */}
        <div className="flex flex-col gap-4">
            <h2 className="text-xl font-bold text-text-primary">Transakce</h2>
            <HalSubresourceProvider subresourceLinkName={"transactions"}>
                <HalEmbeddedTable collectionName={"transactionItemResponseList"}>
                    <TableCell sortable column="date">Datum</TableCell>
                    <TableCell column="amount">Castka</TableCell>
                    <TableCell column="note">Poznamka</TableCell>
                </HalEmbeddedTable>
            </HalSubresourceProvider>
        </div>

        {/* Action buttons from resource templates */}
        <div className="flex gap-3">
            <HalFormButton name={"deposit"}/>
        </div>
    </div>;
}

const MemberName = () => {
    const {resourceData} = useHalRoute();
    const user = resourceData as { firstName: string, lastName: string };
    return <span className="text-text-primary">{user?.firstName || '-'} {user?.lastName || '-'}</span>
}
```

## Pattern 4: Detail Page with Conditional Sections (MemberDetailsPage)

Detail page rendering resource properties directly, with conditional sections and link-based navigation.

Key patterns:
- Cast `resourceData as any` for untyped access to resource fields
- Conditionally render sections based on data presence
- Navigation buttons based on available `_links`
- Use `extractNavigationPath(href)` to convert API hrefs to router paths

```tsx
// Key excerpts from frontend/src/pages/members/MemberDetailsPage.tsx

const { resourceData, isLoading, error, route } = useHalPageData();
const navigate = useNavigate();
const member = resourceData as any;

// Navigate via link
const handleNavigateToLink = (href: string) => {
    navigate(extractNavigationPath(href));
};

// Conditional action buttons based on available links
{member._links?.editOwnMemberInfoForm && (
    <Button onClick={() => handleNavigateToLink(member._links.editOwnMemberInfoForm.href)}>
        Upravit profil
    </Button>
)}

// Display cards with MemberDetailsCard + MemberDetailsField
<MemberDetailsCard title="Zakladni informace">
    <MemberDetailsField label="Jmeno" value={member.firstName}/>
    <MemberDetailsField label="Datum narozeni" value={member.dateOfBirth}
        render={(val) => formatDate(val)}/>
</MemberDetailsCard>
```

## Pattern 5: Custom View with HAL Navigation (CalendarPage)

Custom (non-table) rendering with HAL link navigation (prev/next).

Key patterns:
- Extract items from `_embedded` using type guards
- Navigate via HAL links (`prev`, `next`)
- Custom rendering (calendar grid instead of table)

```tsx
// Key excerpts from frontend/src/pages/calendar/CalendarPage.tsx

const {resourceData, isLoading, error} = useHalPageData();
const navigate = useNavigate();

// Extract embedded items
let calendarItems: CalendarItem[] = [];
if (resourceData && hasCalendarItems(resourceData)) {
    calendarItems = resourceData._embedded.calendarItemDtoList || [];
}

// Navigate via HAL links
const handlePrevMonth = () => {
    const prevLink = resourceData?._links?.prev;
    if (prevLink) navigate(extractNavigationPath(toHref(prevLink)));
};

// Conditionally disable navigation buttons
<button onClick={handlePrevMonth} disabled={!resourceData?._links?.prev}>
    ←
</button>
```

## Common Imports

```tsx
// Data access
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {useHalRoute, HalSubresourceProvider} from '../../contexts/HalRouteContext.tsx';

// Table components
import {HalEmbeddedTable} from '../../components/HalNavigator2/HalEmbeddedTable.tsx';
import {TableCell} from '../../components/KlabisTable';

// Form components
import {HalFormButton} from '../../components/HalNavigator2/HalFormButton.tsx';
import {type FormStep, MultiStepFormModal} from '../../components/HalNavigator2/MultiStepFormModal.tsx';
import {HalLinksSection} from '../../components/HalNavigator2/HalLinksSection.tsx';

// Types
import type {EntityModel, HalResponse} from '../../api';

// Utilities
import {formatDate} from '../../utils/dateUtils.ts';
import {toHref} from '../../api/hateoas.ts';
import {extractNavigationPath} from '../../utils/navigationPath.ts';

// UI components
import {Alert, Button, Skeleton, Spinner} from '../../components/UI';
```

## Routing

Pages are registered in `frontend/src/App.tsx`:

```tsx
<Route path="/members" element={<MembersPage />} />
<Route path="/members/:memberId" element={<MemberDetailsPage />} />
<Route path="/events" element={<EventsPage />} />
<Route path="/calendar-items" element={<CalendarPage />} />
<Route path="*" element={<GenericHalPage />} />  // fallback
```

All routes are wrapped in `ProtectedRoute` and `HalRouteProvider` which automatically fetches HAL data based on the current URL path.
