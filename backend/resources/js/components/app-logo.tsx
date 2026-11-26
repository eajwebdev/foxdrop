/** The FoxDrop wordmark for the navy sidebar; collapses to the fox mark. */
export default function AppLogo() {
    return (
        <>
            <img
                src="/images/foxdrop_mark_on_dark.webp"
                alt=""
                className="hidden size-8 object-contain group-data-[collapsible=icon]:block"
            />
            <img
                src="/images/foxdrop_wordmark_on_dark.webp"
                alt="FoxDrop"
                className="h-10 w-auto object-contain group-data-[collapsible=icon]:hidden"
            />
            <span className="sr-only">FoxDrop Admin</span>
        </>
    );
}
