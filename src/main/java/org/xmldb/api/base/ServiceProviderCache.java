/*
 * The XML:DB Initiative Software License, Version 1.0
 *
 * Copyright (c) 2000-2022 The XML:DB Initiative. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * 3. The end-user documentation included with the redistribution, if any, must include the
 * following acknowledgment: "This product includes software developed by the XML:DB Initiative
 * (http://www.xmldb.org/)." Alternately, this acknowledgment may appear in the software itself, if
 * and wherever such third-party acknowledgments normally appear.
 *
 * 4. The name "XML:DB Initiative" must not be used to endorse or promote products derived from this
 * software without prior written permission. For written permission, please contact info@xmldb.org.
 *
 * 5. Products derived from this software may not be called "XML:DB", nor may "XML:DB" appear in
 * their name, without prior written permission of the XML:DB Initiative.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 * This software consists of voluntary contributions made by many individuals on behalf of the
 * XML:DB Initiative. For more information on the XML:DB Initiative, please see
 * <https://github.com/xmldb-org/>.
 */
package org.xmldb.api.base;

import static org.xmldb.api.base.ErrorCodes.NO_SUCH_SERVICE;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class ServiceProviderCache implements ServiceProvider {
    private final StampedLock lock;
    private final Consumer<ServiceRegistry> initializer;

    private List<ImplementationProvider<? extends Service>> providers;

    public static <S extends Service> ServiceProviderCache initialize(Consumer<ServiceRegistry> initializer) {
        return new ServiceProviderCache(initializer);
    }

    private ServiceProviderCache(Consumer<ServiceRegistry> initializer) {
        lock = new StampedLock();
        this.initializer = initializer;
    }

    private List<ImplementationProvider<? extends Service>> providers() {
        long stamp = lock.tryOptimisticRead();
        if (stamp > 0) {
            if (lock.validate(stamp) && providers != null) {
                return providers;
            }
        }
        // fallback to locking read
        stamp = lock.readLock();
        try {
            if (providers != null) {
                return providers;
            }
        } finally {
            lock.unlockRead(stamp);
        }
        // create write lock to initialize values
        stamp = lock.writeLock();
        try {
            providers = new ArrayList<>();
            initializer.accept(this::addServiceCache);
            return providers;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    private <S extends Service> void addServiceCache(Class<S> serviceType, Supplier<S> serviceSupplier) {
        providers.add(new ImplementationProvider<>(serviceType, serviceSupplier));
    }

    @Override
    public <S extends Service> boolean hasService(Class<S> serviceType) {
        for (ImplementationProvider<? extends Service> provider : providers()) {
            if (provider.test(serviceType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public <S extends Service> Optional<S> findService(Class<S> serviceType) {
        for (ImplementationProvider<? extends Service> provider : providers()) {
            if (provider.test(serviceType)) {
                return Optional.ofNullable(provider.instance(serviceType));
            }
        }
        return Optional.empty();
    }

    @Override
    public <S extends Service> S getService(Class<S> serviceType) throws XMLDBException {
        return findService(serviceType).orElseThrow(() -> new XMLDBException(NO_SUCH_SERVICE));
    }

    static final class ImplementationProvider<S extends Service> implements Predicate<Class<?>> {
        private final Class<S> serviceType;
        private final Supplier<S> serviceSupplier;

        ImplementationProvider(Class<S> serviceType, Supplier<S> serviceSupplier) {
            this.serviceType = serviceType;
            this.serviceSupplier = serviceSupplier;
        }

        @Override
        public boolean test(Class<?> tested) {
            return serviceType.isAssignableFrom(tested);
        }

        @SuppressWarnings("unchecked")
        <I extends Service> I instance(Class<I> serviceType) {
            return (I) serviceSupplier.get();
        }
    }

    /**
     * Registry used to add available service providers.
     */
    @FunctionalInterface
    public interface ServiceRegistry {
        /**
         * Registers the given service supplier for the given service type.
         * 
         * @param serviceType
         *            the service type
         * @param serviceSupplier
         *            the supplier for the implementation instance
         */
        <S extends Service> void add(Class<S> serviceType, Supplier<S> serviceSupplier);
    }
}