export class Utils {
    static safeApply (that: any) {
        return new Promise((resolve) => {
            let phase = (that.$root !== null)?that.$root.$$phase : undefined;
            if(phase === '$apply' || phase === '$digest') {
                if(resolve && (typeof(resolve) === 'function')) resolve();
            } else {
                if (resolve && (typeof(resolve) === 'function')) that.$apply(resolve);
                else that.$apply();
            }
        });
    }
}